output "url_invoke" {
  value = aws_api_gateway_stage.v1.invoke_url
}

output "rest_api_id" {
  value = aws_api_gateway_rest_api.this.id
}

output "nome_api" {
  value = aws_api_gateway_rest_api.this.name
}

output "stage" {
  value = aws_api_gateway_stage.v1.stage_name
}

output "security_group_vpc_link" {
  value = aws_security_group.vpc_link.id
}

output "dominio_regional" {
  description = "Alvo do registro DNS (CNAME/alias) do dominio customizado"
  value       = try(aws_api_gateway_domain_name.this[0].regional_domain_name, null)
}

output "ids_chaves_clientes" {
  value = { for k, c in aws_api_gateway_api_key.clientes : k => c.id }
}
