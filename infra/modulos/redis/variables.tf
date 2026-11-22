variable "nome" {
  description = "Identificador"
  type        = string
}

variable "vpc_id" {
  description = "VPC"
  type        = string
}

variable "subnets" {
  description = "Sub-redes privadas de dados"
  type        = list(string)
}

variable "security_groups_clientes" {
  description = "Security groups autorizados"
  type        = list(string)
}

variable "kms_arn" {
  description = "Chave KMS"
  type        = string
}

variable "tipo_no" {
  description = "Tipo do no"
  type        = string
  default     = "cache.t4g.small"
}

variable "replicas" {
  description = "Replicas (prod: 1 em outra AZ)"
  type        = number
  default     = 1
}

variable "auth_token" {
  description = "Token AUTH (vem do modulo secrets)"
  type        = string
  sensitive   = true
}
