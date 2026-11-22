variable "nome" {
  description = "Nome do cluster e prefixo"
  type        = string
}

variable "vpc_id" {
  description = "VPC"
  type        = string
}

variable "cidr_vpc" {
  description = "CIDR da VPC"
  type        = string
}

variable "subnets" {
  description = "Sub-redes privadas de aplicacao"
  type        = list(string)
}

variable "security_group_alb" {
  description = "Security group do ALB"
  type        = string
}

variable "target_group_arn" {
  description = "Target group da API"
  type        = string
}

variable "kms_arn" {
  description = "Chave KMS de logs e segredos"
  type        = string
}

variable "imagem_api" {
  description = "Imagem inicial da API (o CD atualiza)"
  type        = string
}

variable "imagem_worker" {
  description = "Imagem do worker"
  type        = string
}

variable "jdbc_url" {
  description = "URL JDBC do RDS"
  type        = string
}

variable "redis_host" {
  description = "Endpoint primario do Redis"
  type        = string
}

variable "segredo_banco_api_arn" {
  description = "Segredo do usuario da API"
  type        = string
}

variable "segredo_banco_worker_arn" {
  description = "Segredo do usuario do worker"
  type        = string
}

variable "segredo_banco_dono_arn" {
  description = "Segredo do usuario dono (gerenciado pelo RDS)"
  type        = string
}

variable "segredo_redis_arn" {
  description = "Segredo do AUTH do Redis"
  type        = string
}

variable "segredo_chaves_api_arn" {
  description = "Segredo com os hashes das chaves de API"
  type        = string
}

variable "namespace_metricas" {
  description = "Namespace das metricas customizadas"
  type        = string
  default     = "TransacoesGeopoliticas"
}

variable "api_cpu" {
  description = "CPU da tarefa da API"
  type        = number
  default     = 1024
}

variable "api_memoria" {
  description = "Memoria da tarefa da API (MiB)"
  type        = number
  default     = 2048
}

variable "api_minimo" {
  description = "Minimo de tarefas da API"
  type        = number
  default     = 2
}

variable "api_maximo" {
  description = "Maximo de tarefas da API"
  type        = number
  default     = 10
}

variable "retencao_logs_dias" {
  description = "Retencao dos logs"
  type        = number
  default     = 90
}
