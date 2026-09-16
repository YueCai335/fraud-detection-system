output "ecr_repositories" {
  value = { for k, r in aws_ecr_repository.repos : k => r.repository_url }
}

output "ecs_cluster" {
  value = aws_ecs_cluster.main.name
}

output "ecs_service" {
  value = aws_ecs_service.app.name
}

output "task_family" {
  value = aws_ecs_task_definition.app.family
}

output "github_deploy_role_arn" {
  value = aws_iam_role.github_deploy.arn
}

output "rds_endpoint" {
  value = aws_db_instance.mysql.address
}

output "log_group" {
  value = aws_cloudwatch_log_group.app.name
}
