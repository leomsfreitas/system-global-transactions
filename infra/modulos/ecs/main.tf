# Cluster ECS Fargate: servico da API (autoscaling por CPU) e definicoes de tarefa do worker e da migracao.
data "aws_region" "atual" {}

resource "aws_ecs_cluster" "this" {
  name = var.nome
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_cloudwatch_log_group" "this" {
  for_each          = toset(["api", "worker", "migracao"])
  name              = "/ecs/${var.nome}/${each.key}"
  retention_in_days = var.retencao_logs_dias
  kms_key_id        = var.kms_arn
}

# ---------- Security groups
resource "aws_security_group" "api" {
  name        = "${var.nome}-api"
  description = "Tarefas da API: entrada so do ALB"
  vpc_id      = var.vpc_id
}

resource "aws_vpc_security_group_ingress_rule" "api_alb" {
  security_group_id            = aws_security_group.api.id
  referenced_security_group_id = var.security_group_alb
  from_port                    = 8080
  to_port                      = 8080
  ip_protocol                  = "tcp"
  description                  = "HTTP do ALB"
}

resource "aws_security_group" "worker" {
  name        = "${var.nome}-worker"
  description = "Tarefas do worker: sem entrada"
  vpc_id      = var.vpc_id
}

# Saida: HTTPS (fontes externas, AWS APIs, ECR), PostgreSQL e Redis dentro da VPC.
resource "aws_vpc_security_group_egress_rule" "https" {
  for_each          = { api = aws_security_group.api.id, worker = aws_security_group.worker.id }
  security_group_id = each.value
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
  description       = "HTTPS de saida"
}

resource "aws_vpc_security_group_egress_rule" "dados" {
  for_each = {
    api-pg     = { sg = aws_security_group.api.id, porta = 5432 }
    api-redis  = { sg = aws_security_group.api.id, porta = 6379 }
    worker-pg  = { sg = aws_security_group.worker.id, porta = 5432 }
    worker-red = { sg = aws_security_group.worker.id, porta = 6379 }
  }
  security_group_id = each.value.sg
  cidr_ipv4         = var.cidr_vpc
  from_port         = each.value.porta
  to_port           = each.value.porta
  ip_protocol       = "tcp"
  description       = "Banco e cache na VPC"
}

# ---------- IAM (menor privilegio, roles separadas por tarefa)
data "aws_iam_policy_document" "assumir_ecs" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "execucao" {
  for_each           = local.segredos_por_tarefa
  name               = "${var.nome}-${each.key}-execucao"
  assume_role_policy = data.aws_iam_policy_document.assumir_ecs.json
}

resource "aws_iam_role_policy_attachment" "execucao" {
  for_each   = aws_iam_role.execucao
  role       = each.value.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Cada tarefa le so os segredos de que precisa.
resource "aws_iam_role_policy" "segredos" {
  for_each = local.segredos_por_tarefa
  name     = "ler-segredos"
  role     = aws_iam_role.execucao[each.key].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      { Effect = "Allow", Action = ["secretsmanager:GetSecretValue"], Resource = each.value },
      { Effect = "Allow", Action = ["kms:Decrypt"], Resource = [var.kms_arn] }
    ]
  })
}

resource "aws_iam_role" "tarefa" {
  for_each           = toset(["api", "worker"])
  name               = "${var.nome}-${each.key}-tarefa"
  assume_role_policy = data.aws_iam_policy_document.assumir_ecs.json
}

resource "aws_iam_role_policy" "metricas" {
  for_each = aws_iam_role.tarefa
  name     = "publicar-metricas"
  role     = each.value.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = ["cloudwatch:PutMetricData"]
      Resource  = "*"
      Condition = { StringEquals = { "cloudwatch:namespace" = var.namespace_metricas } }
    }]
  })
}

locals {
  segredos_por_tarefa = {
    api      = [var.segredo_banco_api_arn, var.segredo_redis_arn, var.segredo_chaves_api_arn]
    worker   = [var.segredo_banco_worker_arn, var.segredo_redis_arn]
    migracao = [var.segredo_banco_dono_arn]
  }

  ambiente_comum = [
    { name = "SPRING_PROFILES_ACTIVE", value = "aws" },
    { name = "DB_URL", value = var.jdbc_url },
    { name = "REDIS_HOST", value = var.redis_host },
    { name = "REDIS_TLS", value = "true" },
    { name = "METRICAS_CLOUDWATCH_NAMESPACE", value = var.namespace_metricas },
  ]

  log = {
    for k, g in aws_cloudwatch_log_group.this : k => {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = g.name
        "awslogs-region"        = data.aws_region.atual.region
        "awslogs-stream-prefix" = k
      }
    }
  }
}

# ---------- Definicoes de tarefa
resource "aws_ecs_task_definition" "api" {
  family                   = "${var.nome}-api"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.api_cpu
  memory                   = var.api_memoria
  execution_role_arn       = aws_iam_role.execucao["api"].arn
  task_role_arn            = aws_iam_role.tarefa["api"].arn
  runtime_platform {
    cpu_architecture        = "ARM64"
    operating_system_family = "LINUX"
  }
  container_definitions = jsonencode([{
    name                   = "api"
    image                  = var.imagem_api
    essential              = true
    readonlyRootFilesystem = true
    portMappings           = [{ containerPort = 8080, protocol = "tcp" }]
    environment            = concat(local.ambiente_comum, [{ name = "DB_USUARIO_API", value = "transacoes_api" }])
    secrets = [
      { name = "DB_SENHA_API", valueFrom = "${var.segredo_banco_api_arn}:senha::" },
      { name = "REDIS_SENHA", valueFrom = var.segredo_redis_arn },
      { name = "API_KEYS_SHA256", valueFrom = var.segredo_chaves_api_arn },
    ]
    mountPoints      = [{ sourceVolume = "tmp", containerPath = "/tmp" }]
    logConfiguration = local.log["api"]
    healthCheck = {
      command     = ["CMD-SHELL", "wget -qO- http://localhost:8080/actuator/health/liveness || exit 1"]
      interval    = 15
      retries     = 3
      startPeriod = 60
    }
  }])
  volume {
    name = "tmp"
  }
}

resource "aws_ecs_task_definition" "worker" {
  family                   = "${var.nome}-worker"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.execucao["worker"].arn
  task_role_arn            = aws_iam_role.tarefa["worker"].arn
  runtime_platform {
    cpu_architecture        = "ARM64"
    operating_system_family = "LINUX"
  }
  container_definitions = jsonencode([{
    name                   = "worker"
    image                  = var.imagem_worker
    essential              = true
    readonlyRootFilesystem = true
    command                = ["--job=ManutencaoParticoesJob"]
    environment            = concat(local.ambiente_comum, [{ name = "DB_USUARIO_WORKER", value = "transacoes_worker" }])
    secrets = [
      { name = "DB_SENHA_WORKER", valueFrom = "${var.segredo_banco_worker_arn}:senha::" },
      { name = "REDIS_SENHA", valueFrom = var.segredo_redis_arn },
    ]
    mountPoints      = [{ sourceVolume = "tmp", containerPath = "/tmp" }]
    logConfiguration = local.log["worker"]
  }])
  volume {
    name = "tmp"
  }
}

# Migracao: mesma imagem do worker, com o usuario dono (segredo gerenciado pelo RDS).
resource "aws_ecs_task_definition" "migracao" {
  family                   = "${var.nome}-migracao"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.execucao["migracao"].arn
  task_role_arn            = aws_iam_role.tarefa["worker"].arn
  runtime_platform {
    cpu_architecture        = "ARM64"
    operating_system_family = "LINUX"
  }
  container_definitions = jsonencode([{
    name                   = "migracao"
    image                  = var.imagem_worker
    essential              = true
    readonlyRootFilesystem = true
    command                = ["--job=MigracaoJob"]
    environment = concat(local.ambiente_comum, [
      { name = "MIGRACAO_DB_URL", value = "${var.jdbc_url}?sslmode=require" },
      { name = "DB_USUARIO_WORKER", value = "transacoes_owner" },
    ])
    secrets = [
      { name = "MIGRACAO_DB_USUARIO", valueFrom = "${var.segredo_banco_dono_arn}:username::" },
      { name = "MIGRACAO_DB_SENHA", valueFrom = "${var.segredo_banco_dono_arn}:password::" },
      { name = "DB_SENHA_WORKER", valueFrom = "${var.segredo_banco_dono_arn}:password::" },
    ]
    mountPoints      = [{ sourceVolume = "tmp", containerPath = "/tmp" }]
    logConfiguration = local.log["migracao"]
  }])
  volume {
    name = "tmp"
  }
}

# ---------- Servico da API
resource "aws_ecs_service" "api" {
  name                              = "api"
  cluster                           = aws_ecs_cluster.this.id
  task_definition                   = aws_ecs_task_definition.api.arn
  desired_count                     = var.api_minimo
  launch_type                       = "FARGATE"
  health_check_grace_period_seconds = 60
  enable_execute_command            = false
  propagate_tags                    = "SERVICE"

  network_configuration {
    subnets          = var.subnets
    security_groups  = [aws_security_group.api.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.target_group_arn
    container_name   = "api"
    container_port   = 8080
  }

  # Rolling update com rollback automatico se as tarefas novas nao ficarem saudaveis.
  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200
  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  lifecycle {
    # O CD troca a definicao de tarefa (nova imagem); o autoscaling troca a quantidade.
    ignore_changes = [desired_count, task_definition]
  }
}

resource "aws_appautoscaling_target" "api" {
  service_namespace  = "ecs"
  resource_id        = "service/${aws_ecs_cluster.this.name}/${aws_ecs_service.api.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  min_capacity       = var.api_minimo
  max_capacity       = var.api_maximo
}

resource "aws_appautoscaling_policy" "cpu" {
  name               = "${var.nome}-api-cpu-60"
  policy_type        = "TargetTrackingScaling"
  service_namespace  = aws_appautoscaling_target.api.service_namespace
  resource_id        = aws_appautoscaling_target.api.resource_id
  scalable_dimension = aws_appautoscaling_target.api.scalable_dimension
  target_tracking_scaling_policy_configuration {
    target_value       = 60
    scale_in_cooldown  = 120
    scale_out_cooldown = 60
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
  }
}
