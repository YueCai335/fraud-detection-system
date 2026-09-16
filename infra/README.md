# infra

Terraform for the on-demand AWS environment (ECS Fargate + RDS MySQL). The environment is created
for verification/demo sessions and destroyed afterwards; nothing here is meant to run 24/7.

```
infra/
├── bootstrap/   S3 bucket for Terraform state — applied once, kept
└── env/         everything else — apply / destroy per session
```

## First time

```bash
cd infra/bootstrap
AWS_PROFILE=fraud terraform init && AWS_PROFILE=fraud terraform apply
```

## Each session

```bash
cd infra/env
cp terraform.tfvars.example terraform.tfvars     # set allowed_cidr to your IP/32
AWS_PROFILE=fraud terraform init
AWS_PROFILE=fraud terraform apply                # ~10–15 min, RDS is the slow part
# build + push images and roll the service: GitHub Actions "deploy" workflow
# verify: scripts/smoke.sh http://<task public ip>:8080 demo demo123
AWS_PROFILE=fraud terraform destroy
```

Resources: VPC (2 public + 2 private subnets, no NAT), ECR ×2, ECS cluster + Fargate service
(1 task: fraud-service + model-service sidecar), RDS MySQL db.t4g.micro (private, password in
Secrets Manager), S3 bucket for batch job files (private, 7-day expiry), CloudWatch log group,
IAM (ECS execution role, ECS task role scoped to the bucket, GitHub OIDC deploy role).

See `docs/deployment.md` for the log of actual deployments, timings and cost.
