output "vpc_id" {
  value = aws_vpc.this.id
}

output "cidr" {
  value = aws_vpc.this.cidr_block
}

output "subnets_publicas" {
  value = aws_subnet.publica[*].id
}

output "subnets_aplicacao" {
  value = aws_subnet.aplicacao[*].id
}

output "subnets_dados" {
  value = aws_subnet.dados[*].id
}
