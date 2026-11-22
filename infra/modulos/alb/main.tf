# ALB interno (sub-redes privadas): so o VPC Link v2 do API Gateway chega aqui (ADR 0017).
resource "aws_security_group" "alb" {
  name        = "${var.nome}-alb"
  description = "ALB interno: entrada so do VPC Link do API Gateway"
  vpc_id      = var.vpc_id
}

resource "aws_vpc_security_group_ingress_rule" "alb" {
  security_group_id            = aws_security_group.alb.id
  referenced_security_group_id = var.security_group_vpc_link
  from_port                    = 80
  to_port                      = 80
  ip_protocol                  = "tcp"
  description                  = "HTTP do VPC Link"
}

resource "aws_vpc_security_group_egress_rule" "alb" {
  security_group_id = aws_security_group.alb.id
  cidr_ipv4         = var.cidr_vpc
  from_port         = 8080
  to_port           = 8080
  ip_protocol       = "tcp"
  description       = "Encaminha para as tarefas da API"
}

resource "aws_lb" "this" {
  name                       = var.nome
  internal                   = true
  load_balancer_type         = "application"
  subnets                    = var.subnets
  security_groups            = [aws_security_group.alb.id]
  drop_invalid_header_fields = true
  enable_deletion_protection = var.protecao_exclusao

  access_logs {
    bucket  = var.bucket_logs
    prefix  = "alb"
    enabled = var.bucket_logs != ""
  }
}

resource "aws_lb_target_group" "api" {
  name                 = "${var.nome}-api"
  port                 = 8080
  protocol             = "HTTP"
  target_type          = "ip"
  vpc_id               = var.vpc_id
  deregistration_delay = 30

  health_check {
    path                = "/actuator/health/readiness"
    matcher             = "200"
    interval            = 15
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

# Trafego interno a VPC: Gateway -> VPC Link -> ALB em HTTP. TLS ponta a ponta exigiria certificado
# privado (ACM PCA); registrado como risco aceito na auditoria de seguranca (AT12).
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn
  }
}
