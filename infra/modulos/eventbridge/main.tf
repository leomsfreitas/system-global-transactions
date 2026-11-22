# EventBridge Scheduler dispara tarefas ECS do worker com --job=NOME (secao 5.6, ADR 0011).
locals {
  agendas = {
    cotacoes-bcb = { job = "AtualizacaoCotacoesBcbJob", expressao = "cron(0/15 9-17 ? * MON-FRI *)", fuso = "America/Sao_Paulo" }
    cotacoes-bce = { job = "AtualizacaoCotacoesBceJob", expressao = "cron(30 17 ? * MON-FRI *)", fuso = "Europe/Berlin" }
    indicadores  = { job = "AtualizacaoIndicadoresJob", expressao = "cron(0 6 * * ? *)", fuso = "UTC" }
    indice       = { job = "CalculoIndiceGeopoliticoJob", expressao = "cron(30 6 * * ? *)", fuso = "UTC" }
    particoes    = { job = "ManutencaoParticoesJob", expressao = "cron(0 3 * * ? *)", fuso = "UTC" }
  }
}

resource "aws_iam_role" "agendador" {
  name = "${var.nome}-agendador"
  assume_role_policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [{ Effect = "Allow", Principal = { Service = "scheduler.amazonaws.com" }, Action = "sts:AssumeRole" }]
  })
}

resource "aws_iam_role_policy" "agendador" {
  name = "executar-worker"
  role = aws_iam_role.agendador.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Action    = ["ecs:RunTask"]
        Resource  = ["${var.task_definition_family_arn}:*"]
        Condition = { ArnEquals = { "ecs:cluster" = var.cluster_arn } }
      },
      { Effect = "Allow", Action = ["iam:PassRole"], Resource = var.roles_tarefa_arns },
      { Effect = "Allow", Action = ["kms:Decrypt"], Resource = [var.kms_arn] }
    ]
  })
}

resource "aws_scheduler_schedule_group" "this" {
  name = var.nome
}

resource "aws_scheduler_schedule" "jobs" {
  for_each                     = local.agendas
  name                         = each.key
  group_name                   = aws_scheduler_schedule_group.this.name
  schedule_expression          = each.value.expressao
  schedule_expression_timezone = each.value.fuso
  state                        = var.habilitado ? "ENABLED" : "DISABLED"
  kms_key_arn                  = var.kms_arn

  flexible_time_window {
    mode = "OFF"
  }

  target {
    arn      = var.cluster_arn
    role_arn = aws_iam_role.agendador.arn
    input = jsonencode({
      containerOverrides = [{ name = "worker", command = ["--job=${each.value.job}"] }]
    })

    ecs_parameters {
      task_definition_arn = var.task_definition_family_arn
      launch_type         = "FARGATE"
      task_count          = 1
      network_configuration {
        subnets          = var.subnets
        security_groups  = [var.security_group_worker]
        assign_public_ip = false
      }
    }

    retry_policy {
      maximum_retry_attempts       = 2
      maximum_event_age_in_seconds = 600
    }
  }
}
