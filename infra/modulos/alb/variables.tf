variable "nome" {
  description = "Nome do ALB (ate 32 caracteres)"
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

variable "security_group_vpc_link" {
  description = "Security group do VPC Link do API Gateway"
  type        = string
}

variable "bucket_logs" {
  description = "Bucket S3 de access logs (vazio desliga)"
  type        = string
  default     = ""
}

variable "protecao_exclusao" {
  description = "Deletion protection"
  type        = bool
  default     = true
}
