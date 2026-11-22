# Roles assumidas pelo GitHub Actions via OIDC (sem chaves estaticas). Uma para CD (ECR/ECS) e uma
# para Terraform (plan em PR com leitura; apply so no environment protegido).
data "aws_caller_identity" "atual" {}

resource "aws_iam_openid_connect_provider" "github" {
  count          = var.criar_provedor ? 1 : 0
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
}

locals {
  provedor_arn = var.criar_provedor ? aws_iam_openid_connect_provider.github[0].arn : "arn:aws:iam::${data.aws_caller_identity.atual.account_id}:oidc-provider/token.actions.githubusercontent.com"
}

data "aws_iam_policy_document" "confianca" {
  for_each = { cd = var.sujeitos_cd, terraform = var.sujeitos_terraform }
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [local.provedor_arn]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = each.value
    }
  }
}

resource "aws_iam_role" "cd" {
  name                 = "${var.nome}-github-cd"
  assume_role_policy   = data.aws_iam_policy_document.confianca["cd"].json
  max_session_duration = 3600
}

resource "aws_iam_role_policy" "cd" {
  name = "deploy"
  role = aws_iam_role.cd.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      { Effect = "Allow", Action = ["ecr:GetAuthorizationToken"], Resource = "*" },
      {
        Effect   = "Allow"
        Action   = ["ecr:BatchCheckLayerAvailability", "ecr:InitiateLayerUpload", "ecr:UploadLayerPart", "ecr:CompleteLayerUpload", "ecr:PutImage", "ecr:BatchGetImage", "ecr:DescribeImageScanFindings", "ecr:DescribeImages"]
        Resource = var.repositorios_ecr_arns
      },
      # Register/DescribeTaskDefinition nao aceitam restricao por recurso na AWS.
      { Effect = "Allow", Action = ["ecs:DescribeTaskDefinition", "ecs:RegisterTaskDefinition"], Resource = "*" },
      {
        Effect   = "Allow"
        Action   = ["ecs:DescribeServices", "ecs:UpdateService"]
        Resource = "arn:aws:ecs:*:${data.aws_caller_identity.atual.account_id}:service/${var.cluster_nome}/*"
      },
      {
        Effect    = "Allow"
        Action    = ["ecs:RunTask"]
        Resource  = "arn:aws:ecs:*:${data.aws_caller_identity.atual.account_id}:task-definition/${var.nome}-*"
        Condition = { ArnEquals = { "ecs:cluster" = var.cluster_arn } }
      },
      {
        Effect   = "Allow"
        Action   = ["ecs:DescribeTasks"]
        Resource = "arn:aws:ecs:*:${data.aws_caller_identity.atual.account_id}:task/${var.cluster_nome}/*"
      },
      { Effect = "Allow", Action = ["iam:PassRole"], Resource = var.roles_tarefa_arns }
    ]
  })
}

resource "aws_iam_role" "terraform" {
  name                 = "${var.nome}-github-terraform"
  assume_role_policy   = data.aws_iam_policy_document.confianca["terraform"].json
  max_session_duration = 3600
}

# Plan precisa de leitura ampla; apply usa a mesma role, liberada so no environment protegido do GitHub.
resource "aws_iam_role_policy_attachment" "terraform_leitura" {
  role       = aws_iam_role.terraform.name
  policy_arn = "arn:aws:iam::aws:policy/ReadOnlyAccess"
}

resource "aws_iam_role_policy_attachment" "terraform_escrita" {
  count      = var.permitir_apply ? 1 : 0
  role       = aws_iam_role.terraform.name
  policy_arn = "arn:aws:iam::aws:policy/PowerUserAccess"
}
