variable "nome" {
  description = "Prefixo dos recursos (ex.: transacoes-prod)"
  type        = string
}

variable "conta_id" {
  description = "ID da conta AWS"
  type        = string
}

variable "producao" {
  description = "Liga Multi-AZ, replica do Redis, NAT por AZ e protecao contra exclusao"
  type        = bool
}

variable "azs" {
  description = "Duas AZs do ambiente"
  type        = list(string)
  default     = ["sa-east-1a", "sa-east-1c"]
}

variable "cidr_vpc" {
  description = "CIDR da VPC"
  type        = string
}

variable "tag_imagem" {
  description = "Tag inicial das imagens (o CD atualiza as definicoes de tarefa)"
  type        = string
  default     = "inicial"
}

variable "classe_rds" {
  description = "Classe do RDS"
  type        = string
}

variable "tipo_no_redis" {
  description = "Tipo do no do Redis"
  type        = string
}

variable "api_minimo" {
  description = "Minimo de tarefas da API"
  type        = number
}

variable "api_maximo" {
  description = "Maximo de tarefas da API"
  type        = number
}

variable "api_cpu" {
  description = "CPU da tarefa da API"
  type        = number
  default     = 1024
}

variable "api_memoria" {
  description = "Memoria da tarefa da API"
  type        = number
  default     = 2048
}

variable "dominio" {
  description = "Dominio customizado da API"
  type        = string
  default     = ""
}

variable "certificado_arn" {
  description = "Certificado ACM do dominio"
  type        = string
  default     = ""
}

variable "clientes" {
  description = "Clientes iniciais (uma chave de API cada)"
  type        = list(string)
  default     = []
}

variable "emails_alarme" {
  description = "E-mails dos alarmes"
  type        = list(string)
  default     = []
}

variable "bucket_logs_alb" {
  description = "Bucket de access logs do ALB"
  type        = string
  default     = ""
}

variable "agendamentos_habilitados" {
  description = "Liga os schedules do worker"
  type        = bool
  default     = true
}

variable "repositorio_github" {
  description = "org/repositorio no GitHub"
  type        = string
}

variable "ambiente_github" {
  description = "Environment do GitHub autorizado a fazer deploy"
  type        = string
}

variable "criar_provedor_oidc" {
  description = "Cria o provedor OIDC do GitHub (so um ambiente por conta)"
  type        = bool
  default     = true
}
