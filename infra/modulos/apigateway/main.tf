# API Gateway REST: chave de API, usage plans, throttling, WAF, dominio customizado e integracao
# privada com o ALB interno via VPC Link v2 (ADR 0017).

resource "aws_security_group" "vpc_link" {
  name        = "${var.nome}-vpc-link"
  description = "VPC Link v2 do API Gateway: so saida para o ALB"
  vpc_id      = var.vpc_id
}

resource "aws_vpc_security_group_egress_rule" "vpc_link" {
  security_group_id = aws_security_group.vpc_link.id
  cidr_ipv4         = var.cidr_vpc
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
  description       = "HTTP para o ALB interno"
}

resource "aws_apigatewayv2_vpc_link" "this" {
  name               = var.nome
  subnet_ids         = var.subnets
  security_group_ids = [aws_security_group.vpc_link.id]
}

resource "aws_api_gateway_rest_api" "this" {
  name                         = var.nome
  description                  = "API de transacoes com ajuste geopolitico"
  disable_execute_api_endpoint = var.dominio != ""
  endpoint_configuration {
    types = ["REGIONAL"]
  }
  lifecycle {
    create_before_destroy = true
  }
}

# Apenas /api/v1/{proxy+}; actuator e swagger ficam fora do Gateway.
resource "aws_api_gateway_resource" "api" {
  rest_api_id = aws_api_gateway_rest_api.this.id
  parent_id   = aws_api_gateway_rest_api.this.root_resource_id
  path_part   = "api"
}

resource "aws_api_gateway_resource" "v1" {
  rest_api_id = aws_api_gateway_rest_api.this.id
  parent_id   = aws_api_gateway_resource.api.id
  path_part   = "v1"
}

resource "aws_api_gateway_resource" "proxy" {
  rest_api_id = aws_api_gateway_rest_api.this.id
  parent_id   = aws_api_gateway_resource.v1.id
  path_part   = "{proxy+}"
}

resource "aws_api_gateway_method" "proxy" {
  rest_api_id        = aws_api_gateway_rest_api.this.id
  resource_id        = aws_api_gateway_resource.proxy.id
  http_method        = "ANY"
  authorization      = "NONE"
  api_key_required   = true
  request_parameters = { "method.request.path.proxy" = true }
}

resource "aws_api_gateway_integration" "proxy" {
  rest_api_id             = aws_api_gateway_rest_api.this.id
  resource_id             = aws_api_gateway_resource.proxy.id
  http_method             = aws_api_gateway_method.proxy.http_method
  type                    = "HTTP_PROXY"
  integration_http_method = "ANY"
  uri                     = "http://${var.nome}.interno/api/v1/{proxy}"
  connection_type         = "VPC_LINK"
  connection_id           = aws_apigatewayv2_vpc_link.this.id
  integration_target      = var.alb_arn
  timeout_milliseconds    = 10000
  request_parameters      = { "integration.request.path.proxy" = "method.request.path.proxy" }
}

resource "aws_api_gateway_deployment" "this" {
  rest_api_id = aws_api_gateway_rest_api.this.id
  triggers = {
    redeploy = sha1(jsonencode([aws_api_gateway_resource.proxy, aws_api_gateway_method.proxy, aws_api_gateway_integration.proxy]))
  }
  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_cloudwatch_log_group" "acesso" {
  name              = "/apigateway/${var.nome}"
  retention_in_days = var.retencao_logs_dias
  kms_key_id        = var.kms_arn
}

resource "aws_api_gateway_stage" "v1" {
  rest_api_id           = aws_api_gateway_rest_api.this.id
  deployment_id         = aws_api_gateway_deployment.this.id
  stage_name            = "v1"
  xray_tracing_enabled  = true
  cache_cluster_enabled = false
  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.acesso.arn
    # Sem a chave de API: apiKeyId e o identificador, nao o valor.
    format = jsonencode({
      requestId = "$context.requestId", ip = "$context.identity.sourceIp", metodo = "$context.httpMethod",
      caminho   = "$context.resourcePath", status = "$context.status", latenciaMs = "$context.responseLatency",
      apiKeyId  = "$context.identity.apiKeyId", erroWaf = "$context.waf.error"
    })
  }
}

resource "aws_api_gateway_method_settings" "todos" {
  rest_api_id = aws_api_gateway_rest_api.this.id
  stage_name  = aws_api_gateway_stage.v1.stage_name
  method_path = "*/*"
  settings {
    metrics_enabled        = true
    logging_level          = "ERROR"
    data_trace_enabled     = false
    throttling_rate_limit  = var.limite_rps
    throttling_burst_limit = var.limite_rajada
  }
}

resource "aws_api_gateway_usage_plan" "padrao" {
  name = "${var.nome}-padrao"
  api_stages {
    api_id = aws_api_gateway_rest_api.this.id
    stage  = aws_api_gateway_stage.v1.stage_name
  }
  throttle_settings {
    rate_limit  = var.limite_rps_cliente
    burst_limit = var.limite_rajada_cliente
  }
  quota_settings {
    limit  = var.cota_mensal_cliente
    period = "MONTH"
  }
}

# Uma chave por cliente inicial. O valor e gerado pela AWS e entregue por canal seguro (checklist de go-live).
resource "aws_api_gateway_api_key" "clientes" {
  for_each = toset(var.clientes)
  name     = "${var.nome}-${each.key}"
  enabled  = true
}

resource "aws_api_gateway_usage_plan_key" "clientes" {
  for_each      = aws_api_gateway_api_key.clientes
  key_id        = each.value.id
  key_type      = "API_KEY"
  usage_plan_id = aws_api_gateway_usage_plan.padrao.id
}

# ---------- WAF com regras gerenciadas
resource "aws_wafv2_web_acl" "this" {
  name  = var.nome
  scope = "REGIONAL"
  default_action {
    allow {}
  }

  dynamic "rule" {
    for_each = {
      AWSManagedRulesCommonRuleSet          = 1
      AWSManagedRulesAmazonIpReputationList = 3
      AWSManagedRulesAnonymousIpList        = 4
    }
    content {
      name     = rule.key
      priority = rule.value
      override_action {
        none {}
      }
      statement {
        managed_rule_group_statement {
          name        = rule.key
          vendor_name = "AWS"
        }
      }
      visibility_config {
        cloudwatch_metrics_enabled = true
        metric_name                = rule.key
        sampled_requests_enabled   = true
      }
    }
  }

  # Explicita (fora do dynamic) porque inclui a protecao contra Log4Shell (CVE-2021-44228).
  rule {
    name     = "AWSManagedRulesKnownBadInputsRuleSet"
    priority = 2
    override_action {
      none {}
    }
    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesKnownBadInputsRuleSet"
        vendor_name = "AWS"
      }
    }
    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "AWSManagedRulesKnownBadInputsRuleSet"
      sampled_requests_enabled   = true
    }
  }

  rule {
    name     = "limite-por-ip"
    priority = 10
    action {
      block {}
    }
    statement {
      rate_based_statement {
        limit              = var.limite_waf_por_ip_5min
        aggregate_key_type = "IP"
      }
    }
    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "limite-por-ip"
      sampled_requests_enabled   = true
    }
  }

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = var.nome
    sampled_requests_enabled   = true
  }
}

resource "aws_wafv2_web_acl_association" "this" {
  resource_arn = aws_api_gateway_stage.v1.arn
  web_acl_arn  = aws_wafv2_web_acl.this.arn
}

resource "aws_cloudwatch_log_group" "waf" {
  # O nome precisa comecar com aws-waf-logs-
  name              = "aws-waf-logs-${var.nome}"
  retention_in_days = var.retencao_logs_dias
  kms_key_id        = var.kms_arn
}

resource "aws_wafv2_web_acl_logging_configuration" "this" {
  resource_arn            = aws_wafv2_web_acl.this.arn
  log_destination_configs = [aws_cloudwatch_log_group.waf.arn]
  redacted_fields {
    single_header {
      name = "x-api-key"
    }
  }
}

# ---------- Dominio customizado (opcional ate o go-live)
resource "aws_api_gateway_domain_name" "this" {
  count                    = var.dominio != "" ? 1 : 0
  domain_name              = var.dominio
  regional_certificate_arn = var.certificado_arn
  security_policy          = "TLS_1_2"
  endpoint_configuration {
    types = ["REGIONAL"]
  }
}

resource "aws_api_gateway_base_path_mapping" "this" {
  count       = var.dominio != "" ? 1 : 0
  api_id      = aws_api_gateway_rest_api.this.id
  stage_name  = aws_api_gateway_stage.v1.stage_name
  domain_name = aws_api_gateway_domain_name.this[0].domain_name
}
