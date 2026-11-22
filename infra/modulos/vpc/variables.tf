variable "nome" {
  description = "Prefixo dos recursos"
  type        = string
}

variable "cidr" {
  description = "CIDR da VPC (/16)"
  type        = string
}

variable "azs" {
  description = "Duas zonas de disponibilidade"
  type        = list(string)
  validation {
    condition     = length(var.azs) == 2
    error_message = "Informe exatamente 2 AZs."
  }
}

variable "nat_por_az" {
  description = "Um NAT por AZ (prod) ou um so (dev, menor custo)"
  type        = bool
  default     = false
}

variable "retencao_logs_dias" {
  description = "Retencao dos flow logs"
  type        = number
  default     = 90
}

variable "kms_logs_arn" {
  description = "Chave KMS dos log groups"
  type        = string
}
