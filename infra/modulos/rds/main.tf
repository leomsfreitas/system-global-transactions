# PostgreSQL Multi-AZ, criptografado com KMS, TLS obrigatorio, backup de 7 dias.
resource "aws_db_subnet_group" "this" {
  name       = var.nome
  subnet_ids = var.subnets
}

resource "aws_security_group" "rds" {
  name        = "${var.nome}-rds"
  description = "PostgreSQL: entrada so da API e do worker"
  vpc_id      = var.vpc_id
}

resource "aws_vpc_security_group_ingress_rule" "rds" {
  for_each                     = toset(var.security_groups_clientes)
  security_group_id            = aws_security_group.rds.id
  referenced_security_group_id = each.value
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
  description                  = "PostgreSQL a partir de ${each.value}"
}

resource "aws_db_parameter_group" "this" {
  name   = var.nome
  family = "postgres17"

  parameter {
    name  = "rds.force_ssl"
    value = "1"
  }
  parameter {
    name  = "log_min_duration_statement"
    value = "500"
  }
  parameter {
    name  = "log_connections"
    value = "1"
  }
}

resource "aws_iam_role" "monitoramento" {
  name = "${var.nome}-rds-monitoramento"
  assume_role_policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [{ Effect = "Allow", Principal = { Service = "monitoring.rds.amazonaws.com" }, Action = "sts:AssumeRole" }]
  })
}

resource "aws_iam_role_policy_attachment" "monitoramento" {
  role       = aws_iam_role.monitoramento.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

resource "aws_db_instance" "this" {
  identifier                            = var.nome
  engine                                = "postgres"
  engine_version                        = var.versao_motor
  instance_class                        = var.classe
  allocated_storage                     = var.armazenamento_gb
  max_allocated_storage                 = var.armazenamento_gb * 4
  storage_type                          = "gp3"
  storage_encrypted                     = true
  kms_key_id                            = var.kms_arn
  db_name                               = "transacoes"
  username                              = "transacoes_owner"
  manage_master_user_password           = true
  master_user_secret_kms_key_id         = var.kms_arn
  multi_az                              = var.multi_az
  db_subnet_group_name                  = aws_db_subnet_group.this.name
  vpc_security_group_ids                = [aws_security_group.rds.id]
  parameter_group_name                  = aws_db_parameter_group.this.name
  publicly_accessible                   = false
  backup_retention_period               = 7
  backup_window                         = "05:00-06:00"
  maintenance_window                    = "sun:06:30-sun:07:30"
  copy_tags_to_snapshot                 = true
  deletion_protection                   = var.protecao_exclusao
  skip_final_snapshot                   = !var.protecao_exclusao
  final_snapshot_identifier             = var.protecao_exclusao ? "${var.nome}-final" : null
  auto_minor_version_upgrade            = true
  iam_database_authentication_enabled   = true
  performance_insights_enabled          = true
  performance_insights_kms_key_id       = var.kms_arn
  performance_insights_retention_period = 7
  monitoring_interval                   = 60
  monitoring_role_arn                   = aws_iam_role.monitoramento.arn
  enabled_cloudwatch_logs_exports       = ["postgresql", "upgrade"]
}
