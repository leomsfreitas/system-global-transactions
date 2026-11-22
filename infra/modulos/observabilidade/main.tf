# Alarmes (notificacao via SNS) e dashboard. Metricas customizadas vem do Micrometer (namespace configuravel).
resource "aws_sns_topic" "alarmes" {
  name              = "${var.nome}-alarmes"
  kms_master_key_id = var.kms_arn
}

resource "aws_sns_topic_subscription" "email" {
  for_each  = toset(var.emails_alarme)
  topic_arn = aws_sns_topic.alarmes.arn
  protocol  = "email"
  endpoint  = each.value
}

locals {
  acoes = [aws_sns_topic.alarmes.arn]
  alb   = { LoadBalancer = var.alb_arn_suffix }
}

resource "aws_cloudwatch_metric_alarm" "api_5xx" {
  alarm_name          = "${var.nome}-api-taxa-5xx"
  alarm_description   = "Mais de 1% das respostas da API com 5xx em 5 minutos"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  threshold           = 1
  treat_missing_data  = "notBreaching"
  alarm_actions       = local.acoes
  ok_actions          = local.acoes
  metric_query {
    id          = "taxa"
    expression  = "100 * erros / MAX([requisicoes, 1])"
    label       = "Percentual de 5xx"
    return_data = true
  }
  metric_query {
    id = "erros"
    metric {
      namespace   = "AWS/ApplicationELB"
      metric_name = "HTTPCode_Target_5XX_Count"
      dimensions  = local.alb
      period      = 300
      stat        = "Sum"
    }
  }
  metric_query {
    id = "requisicoes"
    metric {
      namespace   = "AWS/ApplicationELB"
      metric_name = "RequestCount"
      dimensions  = local.alb
      period      = 300
      stat        = "Sum"
    }
  }
}

resource "aws_cloudwatch_metric_alarm" "api_latencia_p95" {
  alarm_name          = "${var.nome}-api-latencia-p95"
  alarm_description   = "Latencia p95 da API acima de 300 ms (meta da conversao, ADR 0019)"
  namespace           = "AWS/ApplicationELB"
  metric_name         = "TargetResponseTime"
  dimensions          = local.alb
  extended_statistic  = "p95"
  period              = 300
  evaluation_periods  = 3
  threshold           = 0.3
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = local.acoes
}

resource "aws_cloudwatch_metric_alarm" "api_cpu" {
  alarm_name          = "${var.nome}-api-cpu"
  alarm_description   = "CPU media da API acima de 85% (autoscaling nao acompanha)"
  namespace           = "AWS/ECS"
  metric_name         = "CPUUtilization"
  dimensions          = { ClusterName = var.cluster_nome, ServiceName = var.servico_api }
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  threshold           = 85
  comparison_operator = "GreaterThanThreshold"
  alarm_actions       = local.acoes
}

resource "aws_cloudwatch_metric_alarm" "worker_falhas" {
  for_each            = toset(var.jobs)
  alarm_name          = "${var.nome}-worker-falha-${each.key}"
  alarm_description   = "Job ${each.key} registrou falha (fonte fora, resposta invalida ou erro)"
  namespace           = var.namespace_metricas
  metric_name         = "worker.job.falhas.count"
  dimensions          = { job = each.key }
  statistic           = "Sum"
  period              = 3600
  evaluation_periods  = 1
  threshold           = 0
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = local.acoes
}

# Dado desatualizado: nenhum sucesso do job do BCE (fonte diaria) em 2 dias uteis.
resource "aws_cloudwatch_metric_alarm" "dado_desatualizado" {
  alarm_name          = "${var.nome}-dado-desatualizado"
  alarm_description   = "Sem atualizacao bem-sucedida de cotacoes do BCE em 2 dias: a API vai responder 503 apos 4 dias"
  namespace           = var.namespace_metricas
  metric_name         = "worker.job.registros.count"
  dimensions          = { job = "AtualizacaoCotacoesBceJob", tipo = "lidos" }
  statistic           = "Sum"
  period              = 86400
  evaluation_periods  = 2
  threshold           = 1
  comparison_operator = "LessThanThreshold"
  treat_missing_data  = "breaching"
  alarm_actions       = local.acoes
}

resource "aws_cloudwatch_metric_alarm" "rds_conexoes" {
  alarm_name          = "${var.nome}-rds-conexoes"
  alarm_description   = "Conexoes do RDS acima do limite"
  namespace           = "AWS/RDS"
  metric_name         = "DatabaseConnections"
  dimensions          = { DBInstanceIdentifier = var.rds_identificador }
  statistic           = "Maximum"
  period              = 300
  evaluation_periods  = 2
  threshold           = var.limite_conexoes_rds
  comparison_operator = "GreaterThanThreshold"
  alarm_actions       = local.acoes
}

resource "aws_cloudwatch_metric_alarm" "redis_memoria" {
  alarm_name          = "${var.nome}-redis-memoria"
  alarm_description   = "Memoria do Redis acima de 80%"
  namespace           = "AWS/ElastiCache"
  metric_name         = "DatabaseMemoryUsagePercentage"
  dimensions          = { ReplicationGroupId = var.redis_id }
  statistic           = "Maximum"
  period              = 300
  evaluation_periods  = 2
  threshold           = 80
  comparison_operator = "GreaterThanThreshold"
  alarm_actions       = local.acoes
}

resource "aws_cloudwatch_dashboard" "this" {
  dashboard_name = var.nome
  dashboard_body = jsonencode({
    widgets = [
      { type = "metric", x = 0, y = 0, width = 12, height = 6, properties = {
        title   = "Requisicoes e 5xx (ALB)", region = var.regiao, stat = "Sum", period = 60
        metrics = [["AWS/ApplicationELB", "RequestCount", "LoadBalancer", var.alb_arn_suffix], [".", "HTTPCode_Target_5XX_Count", ".", "."]]
      } },
      { type = "metric", x = 12, y = 0, width = 12, height = 6, properties = {
        title   = "Latencia p95 (s)", region = var.regiao, stat = "p95", period = 60
        metrics = [["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", var.alb_arn_suffix]]
      } },
      { type = "metric", x = 0, y = 6, width = 12, height = 6, properties = {
        title   = "CPU e memoria da API", region = var.regiao, stat = "Average", period = 60
        metrics = [["AWS/ECS", "CPUUtilization", "ClusterName", var.cluster_nome, "ServiceName", var.servico_api], [".", "MemoryUtilization", ".", ".", ".", "."]]
      } },
      { type = "metric", x = 12, y = 6, width = 12, height = 6, properties = {
        title   = "Worker: registros gravados por job", region = var.regiao, stat = "Sum", period = 3600
        metrics = [for j in var.jobs : [var.namespace_metricas, "worker.job.registros.count", "job", j, "tipo", "gravados"]]
      } },
      { type = "metric", x = 0, y = 12, width = 12, height = 6, properties = {
        title   = "RDS conexoes e Redis memoria", region = var.regiao, stat = "Maximum", period = 300
        metrics = [["AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", var.rds_identificador], ["AWS/ElastiCache", "DatabaseMemoryUsagePercentage", "ReplicationGroupId", var.redis_id]]
      } }
    ]
  })
}
