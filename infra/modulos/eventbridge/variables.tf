variable "nome" {
  description = "Prefixo"
  type        = string
}

variable "cluster_arn" {
  description = "Cluster ECS"
  type        = string
}

variable "task_definition_family_arn" {
  description = "ARN da familia da definicao de tarefa do worker, sem revisao (sempre a mais recente)"
  type        = string
}

variable "roles_tarefa_arns" {
  description = "Roles que o agendador pode passar para a tarefa"
  type        = list(string)
}

variable "subnets" {
  description = "Sub-redes privadas de aplicacao"
  type        = list(string)
}

variable "security_group_worker" {
  description = "Security group do worker"
  type        = string
}

variable "kms_arn" {
  description = "Chave KMS dos schedules"
  type        = string
}

variable "habilitado" {
  description = "Liga os agendamentos"
  type        = bool
  default     = true
}
