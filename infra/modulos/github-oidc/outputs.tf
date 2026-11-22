output "role_cd_arn" {
  value = aws_iam_role.cd.arn
}

output "role_terraform_arn" {
  value = aws_iam_role.terraform.arn
}
