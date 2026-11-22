output "cluster_arn" {
  value = aws_ecs_cluster.this.arn
}

output "cluster_nome" {
  value = aws_ecs_cluster.this.name
}

output "servico_api" {
  value = aws_ecs_service.api.name
}

output "task_definition_worker_arn" {
  value = aws_ecs_task_definition.worker.arn
}

output "task_definition_worker_family" {
  value = aws_ecs_task_definition.worker.family
}

output "task_definition_migracao_family" {
  value = aws_ecs_task_definition.migracao.family
}

output "security_group_api" {
  value = aws_security_group.api.id
}

output "security_group_worker" {
  value = aws_security_group.worker.id
}

output "roles_arns" {
  value = concat([for r in aws_iam_role.execucao : r.arn], [for r in aws_iam_role.tarefa : r.arn])
}

output "log_groups" {
  value = { for k, g in aws_cloudwatch_log_group.this : k => g.name }
}
