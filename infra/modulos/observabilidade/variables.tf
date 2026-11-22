variable "nome" {
  description = "Prefixo"
  type        = string
}

variable "regiao" {
  description = "Regiao (dashboard)"
  type        = string
}

variable "kms_arn" {
  description = "Chave KMS do topico SNS"
  type        = string
}

variable "emails_alarme" {
  description = "E-mails inscritos nos alarmes"
  type        = list(string)
  default     = []
}

variable "alb_arn_suffix" {
  description = "Sufixo do ARN do ALB"
  type        = string
}

variable "cluster_nome" {
  description = "Cluster ECS"
  type        = string
}

variable "servico_api" {
  description = "Servico ECS da API"
  type        = string
}

variable "rds_identificador" {
  description = "Identificador do RDS"
  type        = string
}

variable "redis_id" {
  description = "Replication group do Redis"
  type        = string
}

variable "namespace_metricas" {
  description = "Namespace das metricas customizadas"
  type        = string
  default     = "TransacoesGeopoliticas"
}

variable "limite_conexoes_rds" {
  description = "Alarme de conexoes do RDS"
  type        = number
  default     = 150
}

variable "jobs" {
  description = "Jobs do worker monitorados"
  type        = list(string)
  default     = ["AtualizacaoCotacoesBcbJob", "AtualizacaoCotacoesBceJob", "AtualizacaoIndicadoresJob", "CalculoIndiceGeopoliticoJob", "ManutencaoParticoesJob"]
}
