# Segredos da aplicacao. A senha do usuario dono do RDS e gerenciada pelo proprio RDS (modulo rds).
# Senhas aleatorias ficam no state, que e criptografado no S3 (bootstrap-estado).
resource "random_password" "banco" {
  for_each = toset(["api", "worker"])
  length   = 32
  special  = false
}

resource "random_password" "redis" {
  length  = 48
  special = false
}

resource "aws_secretsmanager_secret" "banco" {
  for_each                = random_password.banco
  name                    = "${var.nome}/banco/${each.key}"
  kms_key_id              = var.kms_arn
  recovery_window_in_days = var.janela_recuperacao_dias
}

resource "aws_secretsmanager_secret_version" "banco" {
  for_each      = random_password.banco
  secret_id     = aws_secretsmanager_secret.banco[each.key].id
  secret_string = jsonencode({ usuario = "transacoes_${each.key}", senha = each.value.result })
}

resource "aws_secretsmanager_secret" "redis" {
  name                    = "${var.nome}/redis/auth-token"
  kms_key_id              = var.kms_arn
  recovery_window_in_days = var.janela_recuperacao_dias
}

resource "aws_secretsmanager_secret_version" "redis" {
  secret_id     = aws_secretsmanager_secret.redis.id
  secret_string = random_password.redis.result
}

# Hashes SHA-256 das chaves de API aceitas (separados por virgula). O valor real e gravado por um
# operador (ver checklist de go-live); o Terraform cria o segredo com um placeholder e nao o sobrescreve.
resource "aws_secretsmanager_secret" "chaves_api" {
  name                    = "${var.nome}/api/chaves-sha256"
  kms_key_id              = var.kms_arn
  recovery_window_in_days = var.janela_recuperacao_dias
}

resource "aws_secretsmanager_secret_version" "chaves_api" {
  secret_id     = aws_secretsmanager_secret.chaves_api.id
  secret_string = "definir-no-go-live"
  lifecycle {
    ignore_changes = [secret_string]
  }
}
