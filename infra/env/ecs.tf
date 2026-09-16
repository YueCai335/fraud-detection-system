resource "aws_ecs_cluster" "main" {
  name = "${var.name}-cluster"
}

resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${var.name}"
  retention_in_days = 7
}

# One task, two containers. Java talks to Python over localhost; only Java is exposed.
# ECS ignores Dockerfile HEALTHCHECKs, so both checks are declared here explicitly.
locals {
  container_definitions = [
    {
      name         = "model-service"
      image        = "${aws_ecr_repository.repos["model-service"].repository_url}:latest"
      essential    = true
      memory       = 640 # hard limit MiB; sklearn + shap + 2 gunicorn workers
      portMappings = [{ containerPort = 5000, protocol = "tcp" }]
      environment = [
        { name = "FRAUD_THRESHOLD", value = "0.25" },
        { name = "PORT", value = "5000" },
      ]
      healthCheck = {
        command     = ["CMD-SHELL", "python -c \"import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://localhost:5000/health').status==200 else 1)\""]
        interval    = 15
        timeout     = 5
        retries     = 3
        startPeriod = 30
      }
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.app.name
          awslogs-region        = var.region
          awslogs-stream-prefix = "model"
        }
      }
    },
    {
      name         = "fraud-service"
      image        = "${aws_ecr_repository.repos["fraud-service"].repository_url}:latest"
      essential    = true
      memory       = 1280 # hard limit MiB; JVM heap = 75% of this via JAVA_TOOL_OPTIONS
      portMappings = [{ containerPort = 8080, protocol = "tcp" }]
      dependsOn    = [{ containerName = "model-service", condition = "HEALTHY" }]
      environment = [
        { name = "DB_URL", value = local.db_url },
        { name = "DB_USER", value = aws_db_instance.mysql.username },
        { name = "MODEL_SERVICE_URL", value = "http://localhost:5000" },
        { name = "SPRING_PROFILES_ACTIVE", value = var.spring_profiles },
      ]
      secrets = [
        # RDS-managed secret is JSON {"username":..,"password":..}; ":password::" selects the key.
        { name = "DB_PASSWORD", valueFrom = "${local.db_secret_arn}:password::" },
      ]
      healthCheck = {
        command     = ["CMD-SHELL", "curl -fs http://localhost:8080/actuator/health || exit 1"]
        interval    = 15
        timeout     = 5
        retries     = 3
        startPeriod = 90 # Flyway + Spring Boot start-up
      }
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.app.name
          awslogs-region        = var.region
          awslogs-stream-prefix = "app"
        }
      }
    },
  ]
}

resource "aws_ecs_task_definition" "app" {
  family                   = "${var.name}-app"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  container_definitions    = jsonencode(local.container_definitions)

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64" # images are built linux/amd64 in GitHub Actions
  }
}

resource "aws_ecs_service" "app" {
  name            = "${var.name}-app"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  # Single task: stop the old one, then start the new one (no second task = no double cost).
  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 100

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = true
  }

  # Terraform creates the service with the ':latest' placeholder; every real deployment
  # registers a new revision (sha-tagged images) from GitHub Actions. Don't fight over it.
  lifecycle {
    ignore_changes = [task_definition]
  }
}
