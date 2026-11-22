variable "nome" {
  description = "Prefixo"
  type        = string
}

variable "criar_provedor" {
  description = "Cria o provedor OIDC (uma vez por conta)"
  type        = bool
  default     = true
}

variable "sujeitos_cd" {
  description = "Sujeitos OIDC permitidos no CD (ex.: repo:org/repo:environment:prod)"
  type        = list(string)
}

variable "sujeitos_terraform" {
  description = "Sujeitos OIDC permitidos no Terraform"
  type        = list(string)
}

variable "repositorios_ecr_arns" {
  description = "Repositorios ECR que o CD pode publicar"
  type        = list(string)
}

variable "roles_tarefa_arns" {
  description = "Roles das tarefas ECS (PassRole no deploy)"
  type        = list(string)
}

variable "cluster_arn" {
  description = "Cluster ECS do ambiente"
  type        = string
}

variable "cluster_nome" {
  description = "Nome do cluster ECS"
  type        = string
}

variable "kms_arn" {
  description = "Chave KMS dos segredos"
  type        = string
}

variable "permitir_apply" {
  description = "Concede escrita a role de Terraform"
  type        = bool
  default     = true
}
