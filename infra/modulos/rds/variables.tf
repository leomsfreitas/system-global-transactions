variable "nome" {
  description = "Identificador da instancia"
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
  description = "Security groups autorizados (API e worker)"
  type        = list(string)
}

variable "kms_arn" {
  description = "Chave KMS"
  type        = string
}

variable "versao_motor" {
  description = "Versao do PostgreSQL"
  type        = string
  default     = "17.6"
}

variable "classe" {
  description = "Classe da instancia"
  type        = string
}

variable "armazenamento_gb" {
  description = "Armazenamento inicial"
  type        = number
  default     = 50
}

variable "multi_az" {
  description = "Standby em outra AZ"
  type        = bool
  default     = true
}

variable "protecao_exclusao" {
  description = "Deletion protection e snapshot final (prod)"
  type        = bool
  default     = true
}
