output "banco_api_arn" {
  value = aws_secretsmanager_secret.banco["api"].arn
}

output "banco_worker_arn" {
  value = aws_secretsmanager_secret.banco["worker"].arn
}

output "redis_arn" {
  value = aws_secretsmanager_secret.redis.arn
}

output "redis_auth_token" {
  value     = random_password.redis.result
  sensitive = true
}

output "chaves_api_arn" {
  value = aws_secretsmanager_secret.chaves_api.arn
}
