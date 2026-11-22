# VPC com 2 AZs: sub-redes publicas (NAT), privadas de aplicacao e privadas de dados.
# AZs fixas por variavel: o conjunto nao muda sozinho quando a AWS adiciona zonas.
locals {
  azs = var.azs
}

resource "aws_vpc" "this" {
  cidr_block           = var.cidr
  enable_dns_hostnames = true
  enable_dns_support   = true
  tags                 = { Name = var.nome }
}

# Security group padrao sem nenhuma regra (CKV2_AWS_12)
resource "aws_default_security_group" "padrao" {
  vpc_id = aws_vpc.this.id
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id
  tags   = { Name = var.nome }
}

resource "aws_subnet" "publica" {
  count             = 2
  vpc_id            = aws_vpc.this.id
  cidr_block        = cidrsubnet(var.cidr, 8, count.index)
  availability_zone = local.azs[count.index]
  tags              = { Name = "${var.nome}-publica-${local.azs[count.index]}", Camada = "publica" }
}

resource "aws_subnet" "aplicacao" {
  count             = 2
  vpc_id            = aws_vpc.this.id
  cidr_block        = cidrsubnet(var.cidr, 8, 10 + count.index)
  availability_zone = local.azs[count.index]
  tags              = { Name = "${var.nome}-aplicacao-${local.azs[count.index]}", Camada = "aplicacao" }
}

resource "aws_subnet" "dados" {
  count             = 2
  vpc_id            = aws_vpc.this.id
  cidr_block        = cidrsubnet(var.cidr, 8, 20 + count.index)
  availability_zone = local.azs[count.index]
  tags              = { Name = "${var.nome}-dados-${local.azs[count.index]}", Camada = "dados" }
}

resource "aws_eip" "nat" {
  count  = var.nat_por_az ? 2 : 1
  domain = "vpc"
  tags   = { Name = "${var.nome}-nat-${count.index}" }
}

resource "aws_nat_gateway" "this" {
  count         = var.nat_por_az ? 2 : 1
  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.publica[count.index].id
  tags          = { Name = "${var.nome}-nat-${count.index}" }
  depends_on    = [aws_internet_gateway.this]
}

resource "aws_route_table" "publica" {
  vpc_id = aws_vpc.this.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }
  tags = { Name = "${var.nome}-publica" }
}

resource "aws_route_table_association" "publica" {
  count          = 2
  subnet_id      = aws_subnet.publica[count.index].id
  route_table_id = aws_route_table.publica.id
}

resource "aws_route_table" "privada" {
  count  = 2
  vpc_id = aws_vpc.this.id
  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.this[var.nat_por_az ? count.index : 0].id
  }
  tags = { Name = "${var.nome}-privada-${count.index}" }
}

resource "aws_route_table_association" "aplicacao" {
  count          = 2
  subnet_id      = aws_subnet.aplicacao[count.index].id
  route_table_id = aws_route_table.privada[count.index].id
}

# Sub-redes de dados sem rota para a internet.
resource "aws_route_table" "dados" {
  vpc_id = aws_vpc.this.id
  tags   = { Name = "${var.nome}-dados" }
}

resource "aws_route_table_association" "dados" {
  count          = 2
  subnet_id      = aws_subnet.dados[count.index].id
  route_table_id = aws_route_table.dados.id
}

# VPC Flow Logs (auditoria de rede)
resource "aws_cloudwatch_log_group" "flow_logs" {
  name              = "/vpc/${var.nome}/flow-logs"
  retention_in_days = var.retencao_logs_dias
  kms_key_id        = var.kms_logs_arn
}

resource "aws_iam_role" "flow_logs" {
  name = "${var.nome}-flow-logs"
  assume_role_policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [{ Effect = "Allow", Principal = { Service = "vpc-flow-logs.amazonaws.com" }, Action = "sts:AssumeRole" }]
  })
}

resource "aws_iam_role_policy" "flow_logs" {
  name = "escrever-logs"
  role = aws_iam_role.flow_logs.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["logs:CreateLogStream", "logs:PutLogEvents", "logs:DescribeLogStreams"]
      Resource = "${aws_cloudwatch_log_group.flow_logs.arn}:*"
    }]
  })
}

resource "aws_flow_log" "this" {
  vpc_id          = aws_vpc.this.id
  traffic_type    = "ALL"
  iam_role_arn    = aws_iam_role.flow_logs.arn
  log_destination = aws_cloudwatch_log_group.flow_logs.arn
}
