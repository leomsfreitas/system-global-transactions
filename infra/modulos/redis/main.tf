# ElastiCache Redis com replica em outra AZ, criptografia em repouso (KMS) e em transito (TLS).
resource "aws_elasticache_subnet_group" "this" {
  name       = var.nome
  subnet_ids = var.subnets
}

resource "aws_security_group" "redis" {
  name        = "${var.nome}-redis"
  description = "Redis: entrada so da API e do worker"
  vpc_id      = var.vpc_id
}

resource "aws_vpc_security_group_ingress_rule" "redis" {
  for_each                     = toset(var.security_groups_clientes)
  security_group_id            = aws_security_group.redis.id
  referenced_security_group_id = each.value
  from_port                    = 6379
  to_port                      = 6379
  ip_protocol                  = "tcp"
  description                  = "Redis a partir de ${each.value}"
}

resource "aws_elasticache_replication_group" "this" {
  replication_group_id       = var.nome
  description                = "Cache de cotacoes e indices"
  engine                     = "redis"
  engine_version             = "7.1"
  node_type                  = var.tipo_no
  num_cache_clusters         = var.replicas + 1
  automatic_failover_enabled = var.replicas > 0
  multi_az_enabled           = var.replicas > 0
  port                       = 6379
  subnet_group_name          = aws_elasticache_subnet_group.this.name
  security_group_ids         = [aws_security_group.redis.id]
  at_rest_encryption_enabled = true
  kms_key_id                 = var.kms_arn
  transit_encryption_enabled = true
  auth_token                 = var.auth_token
  auto_minor_version_upgrade = true
  snapshot_retention_limit   = 1
  maintenance_window         = "sun:07:30-sun:08:30"
}
