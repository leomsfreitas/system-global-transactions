output "url_api" {
  value = module.apigateway.url_invoke
}

output "dominio_regional" {
  value = module.apigateway.dominio_regional
}

output "ecr" {
  value = module.ecr.urls
}

output "cluster" {
  value = module.ecs.cluster_nome
}

output "servico_api" {
  value = module.ecs.servico_api
}

output "task_definition_migracao" {
  value = module.ecs.task_definition_migracao_family
}

output "subnets_aplicacao" {
  value = module.vpc.subnets_aplicacao
}

output "security_group_worker" {
  value = module.ecs.security_group_worker
}

output "role_cd_arn" {
  value = module.github_oidc.role_cd_arn
}

output "role_terraform_arn" {
  value = module.github_oidc.role_terraform_arn
}

output "topico_alarmes" {
  value = module.observabilidade.topico_alarmes_arn
}
