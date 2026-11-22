output "endereco" {
  value = aws_db_instance.this.address
}

output "jdbc_url" {
  value = "jdbc:postgresql://${aws_db_instance.this.address}:${aws_db_instance.this.port}/transacoes"
}

output "segredo_dono_arn" {
  description = "Segredo gerenciado pelo RDS com a senha do usuario dono (migrations)"
  value       = aws_db_instance.this.master_user_secret[0].secret_arn
}

output "identificador" {
  value = aws_db_instance.this.identifier
}
