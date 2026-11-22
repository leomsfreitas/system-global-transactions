variable "nome" {
  description = "Prefixo dos segredos"
  type        = string
}

variable "kms_arn" {
  description = "Chave KMS dos segredos"
  type        = string
}

variable "janela_recuperacao_dias" {
  description = "Dias para recuperar um segredo apagado"
  type        = number
  default     = 7
}
