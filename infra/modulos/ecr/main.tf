# Repositorios de imagem com scan no push, tags imutaveis e criptografia KMS.
resource "aws_ecr_repository" "this" {
  for_each             = toset(var.repositorios)
  name                 = "${var.nome}/${each.key}"
  image_tag_mutability = "IMMUTABLE"
  force_delete         = false

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "KMS"
    kms_key         = var.kms_arn
  }
}

resource "aws_ecr_lifecycle_policy" "this" {
  for_each   = aws_ecr_repository.this
  repository = each.value.name
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Mantem as ${var.imagens_mantidas} imagens mais recentes"
      selection    = { tagStatus = "any", countType = "imageCountMoreThan", countNumber = var.imagens_mantidas }
      action       = { type = "expire" }
    }]
  })
}
