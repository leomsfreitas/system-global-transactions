variable "nome" {
  description = "Nome da API"
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
  description = "Sub-redes privadas do VPC Link"
  type        = list(string)
}

variable "alb_arn" {
  description = "ALB interno"
  type        = string
}

variable "kms_arn" {
  description = "Chave KMS dos logs"
  type        = string
}

variable "dominio" {
  description = "Dominio customizado (vazio ate o go-live)"
  type        = string
  default     = ""
}

variable "certificado_arn" {
  description = "Certificado ACM regional do dominio"
  type        = string
  default     = ""
}

variable "clientes" {
  description = "Identificadores dos primeiros clientes (uma chave cada)"
  type        = list(string)
  default     = []
}

variable "limite_rps" {
  description = "Throttling do estagio (req/s)"
  type        = number
  default     = 1000
}

variable "limite_rajada" {
  description = "Rajada do estagio"
  type        = number
  default     = 2000
}

variable "limite_rps_cliente" {
  description = "Throttling por chave (usage plan)"
  type        = number
  default     = 200
}

variable "limite_rajada_cliente" {
  description = "Rajada por chave"
  type        = number
  default     = 400
}

variable "cota_mensal_cliente" {
  description = "Cota mensal por chave"
  type        = number
  default     = 10000000
}

variable "limite_waf_por_ip_5min" {
  description = "Requisicoes por IP em 5 minutos antes do bloqueio no WAF"
  type        = number
  default     = 30000
}

variable "retencao_logs_dias" {
  description = "Retencao dos logs"
  type        = number
  default     = 90
}
