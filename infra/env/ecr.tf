resource "aws_ecr_repository" "repos" {
  for_each             = toset(["fraud-service", "model-service"])
  name                 = each.key
  image_tag_mutability = "MUTABLE" # 'latest' moves; sha-* tags are what deployments reference
  force_delete         = true      # destroy removes images too

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "keep_recent" {
  for_each   = aws_ecr_repository.repos
  repository = each.value.name
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "keep the 10 most recent images"
      selection    = { tagStatus = "any", countType = "imageCountMoreThan", countNumber = 10 }
      action       = { type = "expire" }
    }]
  })
}
