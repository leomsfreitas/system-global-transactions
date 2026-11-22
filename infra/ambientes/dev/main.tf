# Ambiente dev: instancias menores, sem Multi-AZ, um NAT so.
terraform {
  # State remoto em S3 com lock nativo (use_lockfile). Bucket criado por infra/bootstrap-estado.
  backend "s3" {
    bucket       = "transacoes-geopoliticas-tfstate"
    key          = "dev/terraform.tfstate"
    region       = "sa-east-1"
    encrypt      = true
    use_lockfile = true
  }
}

provider "aws" {
  region = var.regiao
  default_tags {
    tags = { Projeto = "transacoes-geopoliticas", Ambiente = "dev", GerenciadoPor = "terraform", CentroCusto = "youpay-transacoes" }
  }
}

data "aws_caller_identity" "atual" {}

module "plataforma" {
  source              = "../../modulos/plataforma"
  nome                = "transacoes-dev"
  conta_id            = data.aws_caller_identity.atual.account_id
  producao            = false
  cidr_vpc            = "10.20.0.0/16"
  classe_rds          = "db.t4g.medium"
  tipo_no_redis       = "cache.t4g.micro"
  api_minimo          = 1
  api_maximo          = 3
  api_cpu             = 512
  api_memoria         = 1024
  emails_alarme       = var.emails_alarme
  repositorio_github  = var.repositorio_github
  ambiente_github     = "dev"
  criar_provedor_oidc = true
}

variable "regiao" {
  description = "Regiao AWS"
  type        = string
  default     = "sa-east-1"
}

variable "emails_alarme" {
  description = "E-mails dos alarmes"
  type        = list(string)
  default     = []
}

variable "repositorio_github" {
  description = "org/repositorio"
  type        = string
  default     = "youpay/transacoes-geopoliticas"
}

output "plataforma" {
  value = module.plataforma
}
