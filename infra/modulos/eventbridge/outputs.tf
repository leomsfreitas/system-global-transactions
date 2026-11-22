output "agendas" {
  value = { for k, s in aws_scheduler_schedule.jobs : k => s.arn }
}
