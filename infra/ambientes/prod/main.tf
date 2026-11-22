# Ambiente prod: Multi-AZ, replica do Redis, NAT por AZ, minimo de 2 tarefas da API, protecao contra exclusao.
terraform {
  backend "s3" {
    bucket       = "transacoes-geopoliticas-tfstate"
    key          = "prod/terraform.tfstate"
    region       = "sa-east-1"
    encrypt      = true
    use_lockfile = true
  }
}

provider "aws" {
  region = var.regiao
  default_tags {
    tags = { Projeto = "transacoes-geopoliticas", Ambiente = "prod", GerenciadoPor = "terraform", CentroCusto = "youpay-transacoes" }
  }
}

data "aws_caller_identity" "atual" {}

module "plataforma" {
  source              = "../../modulos/plataforma"
  nome                = "transacoes-prod"
  conta_id            = data.aws_caller_identity.atual.account_id
  producao            = true
  cidr_vpc            = "10.30.0.0/16"
  classe_rds          = "db.m7g.large"
  tipo_no_redis       = "cache.m7g.large"
  api_minimo          = 2
  api_maximo          = 10
  dominio             = var.dominio
  certificado_arn     = var.certificado_arn
  clientes            = var.clientes
  emails_alarme       = var.emails_alarme
  bucket_logs_alb     = var.bucket_logs_alb
  repositorio_github  = var.repositorio_github
  ambiente_github     = "prod"
  criar_provedor_oidc = var.criar_provedor_oidc
}

variable "regiao" {
  description = "Regiao AWS"
  type        = string
  default     = "sa-east-1"
}

variable "dominio" {
  description = "Dominio customizado (ex.: api.transacoes.youpay.com.br)"
  type        = string
  default     = ""
}

variable "certificado_arn" {
  description = "Certificado ACM regional do dominio"
  type        = string
  default     = ""
}

variable "clientes" {
  description = "Primeiros clientes (uma chave de API cada)"
  type        = list(string)
  default     = []
}

variable "emails_alarme" {
  description = "E-mails dos alarmes"
  type        = list(string)
  default     = []
}

variable "bucket_logs_alb" {
  description = "Bucket de access logs do ALB"
  type        = string
  default     = ""
}

variable "repositorio_github" {
  description = "org/repositorio"
  type        = string
  default     = "youpay/transacoes-geopoliticas"
}

variable "criar_provedor_oidc" {
  description = "false se dev e prod estiverem na mesma conta (o provedor ja existe)"
  type        = bool
  default     = true
}

output "plataforma" {
  value = module.plataforma
}
