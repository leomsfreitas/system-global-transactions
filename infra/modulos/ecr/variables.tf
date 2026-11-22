variable "nome" {
  description = "Prefixo dos repositorios"
  type        = string
}

variable "repositorios" {
  description = "Nomes dos repositorios"
  type        = list(string)
  default     = ["api", "worker"]
}

variable "kms_arn" {
  description = "Chave KMS"
  type        = string
}

variable "imagens_mantidas" {
  description = "Quantidade de imagens mantidas por repositorio"
  type        = number
  default     = 30
}
