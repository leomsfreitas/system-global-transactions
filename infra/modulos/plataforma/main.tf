# Composicao completa de um ambiente. dev e prod chamam este modulo com tamanhos diferentes.
data "aws_region" "atual" {}

module "kms" {
  source = "../kms"
  nome   = var.nome
}

module "vpc" {
  source       = "../vpc"
  nome         = var.nome
  cidr         = var.cidr_vpc
  azs          = var.azs
  nat_por_az   = var.producao
  kms_logs_arn = module.kms.arn
}

module "ecr" {
  source  = "../ecr"
  nome    = var.nome
  kms_arn = module.kms.arn
}

module "secrets" {
  source  = "../secrets"
  nome    = var.nome
  kms_arn = module.kms.arn
}

module "apigateway" {
  source          = "../apigateway"
  nome            = var.nome
  vpc_id          = module.vpc.vpc_id
  cidr_vpc        = module.vpc.cidr
  subnets         = module.vpc.subnets_aplicacao
  alb_arn         = module.alb.arn
  kms_arn         = module.kms.arn
  dominio         = var.dominio
  certificado_arn = var.certificado_arn
  clientes        = var.clientes
}

module "alb" {
  source                  = "../alb"
  nome                    = var.nome
  vpc_id                  = module.vpc.vpc_id
  cidr_vpc                = module.vpc.cidr
  subnets                 = module.vpc.subnets_aplicacao
  security_group_vpc_link = module.apigateway.security_group_vpc_link
  protecao_exclusao       = var.producao
  bucket_logs             = var.bucket_logs_alb
}

module "ecs" {
  source                   = "../ecs"
  nome                     = var.nome
  vpc_id                   = module.vpc.vpc_id
  cidr_vpc                 = module.vpc.cidr
  subnets                  = module.vpc.subnets_aplicacao
  security_group_alb       = module.alb.security_group_id
  target_group_arn         = module.alb.target_group_arn
  kms_arn                  = module.kms.arn
  imagem_api               = "${module.ecr.urls["api"]}:${var.tag_imagem}"
  imagem_worker            = "${module.ecr.urls["worker"]}:${var.tag_imagem}"
  jdbc_url                 = module.rds.jdbc_url
  redis_host               = module.redis.endpoint
  segredo_banco_api_arn    = module.secrets.banco_api_arn
  segredo_banco_worker_arn = module.secrets.banco_worker_arn
  segredo_banco_dono_arn   = module.rds.segredo_dono_arn
  segredo_redis_arn        = module.secrets.redis_arn
  segredo_chaves_api_arn   = module.secrets.chaves_api_arn
  api_minimo               = var.api_minimo
  api_maximo               = var.api_maximo
  api_cpu                  = var.api_cpu
  api_memoria              = var.api_memoria
}

module "rds" {
  source                   = "../rds"
  nome                     = var.nome
  vpc_id                   = module.vpc.vpc_id
  subnets                  = module.vpc.subnets_dados
  security_groups_clientes = [module.ecs.security_group_api, module.ecs.security_group_worker]
  kms_arn                  = module.kms.arn
  classe                   = var.classe_rds
  multi_az                 = var.producao
  protecao_exclusao        = var.producao
}

module "redis" {
  source                   = "../redis"
  nome                     = var.nome
  vpc_id                   = module.vpc.vpc_id
  subnets                  = module.vpc.subnets_dados
  security_groups_clientes = [module.ecs.security_group_api, module.ecs.security_group_worker]
  kms_arn                  = module.kms.arn
  tipo_no                  = var.tipo_no_redis
  replicas                 = var.producao ? 1 : 0
  auth_token               = module.secrets.redis_auth_token
}

module "eventbridge" {
  source                     = "../eventbridge"
  nome                       = var.nome
  cluster_arn                = module.ecs.cluster_arn
  task_definition_family_arn = "arn:aws:ecs:${data.aws_region.atual.region}:${var.conta_id}:task-definition/${module.ecs.task_definition_worker_family}"
  roles_tarefa_arns          = module.ecs.roles_arns
  subnets                    = module.vpc.subnets_aplicacao
  security_group_worker      = module.ecs.security_group_worker
  habilitado                 = var.agendamentos_habilitados
  kms_arn                    = module.kms.arn
}

module "observabilidade" {
  source            = "../observabilidade"
  nome              = var.nome
  regiao            = data.aws_region.atual.region
  kms_arn           = module.kms.arn
  emails_alarme     = var.emails_alarme
  alb_arn_suffix    = module.alb.arn_suffix
  cluster_nome      = module.ecs.cluster_nome
  servico_api       = module.ecs.servico_api
  rds_identificador = module.rds.identificador
  redis_id          = module.redis.id
}

module "github_oidc" {
  source                = "../github-oidc"
  nome                  = var.nome
  criar_provedor        = var.criar_provedor_oidc
  sujeitos_cd           = ["repo:${var.repositorio_github}:environment:${var.ambiente_github}"]
  sujeitos_terraform    = ["repo:${var.repositorio_github}:pull_request", "repo:${var.repositorio_github}:environment:${var.ambiente_github}"]
  repositorios_ecr_arns = values(module.ecr.arns)
  roles_tarefa_arns     = module.ecs.roles_arns
  cluster_arn           = module.ecs.cluster_arn
  cluster_nome          = module.ecs.cluster_nome
}
