# Chave KMS do ambiente (RDS, Redis, Secrets Manager, ECR, logs, SNS), com rotacao anual.
data "aws_caller_identity" "atual" {}
data "aws_region" "atual" {}

resource "aws_kms_key" "this" {
  description             = "Chave do ambiente ${var.nome}"
  enable_key_rotation     = true
  deletion_window_in_days = 30
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "Administracao pela conta"
        Effect    = "Allow"
        Principal = { AWS = "arn:aws:iam::${data.aws_caller_identity.atual.account_id}:root" }
        Action    = "kms:*"
        Resource  = "*"
      },
      {
        Sid       = "CloudWatch Logs"
        Effect    = "Allow"
        Principal = { Service = "logs.${data.aws_region.atual.region}.amazonaws.com" }
        Action    = ["kms:Encrypt*", "kms:Decrypt*", "kms:ReEncrypt*", "kms:GenerateDataKey*", "kms:Describe*"]
        Resource  = "*"
      },
      {
        Sid       = "SNS para alarmes do CloudWatch"
        Effect    = "Allow"
        Principal = { Service = "cloudwatch.amazonaws.com" }
        Action    = ["kms:Decrypt", "kms:GenerateDataKey*"]
        Resource  = "*"
      }
    ]
  })
}

resource "aws_kms_alias" "this" {
  name          = "alias/${var.nome}"
  target_key_id = aws_kms_key.this.key_id
}
