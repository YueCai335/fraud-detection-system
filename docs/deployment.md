# Deployment log

The AWS environment is **on demand**: `infra/env` is applied for a verification or demo session
and destroyed afterwards. Each row below proves one specific commit on real AWS infrastructure;
the code keeps moving between rows. See [infra/README.md](../infra/README.md) for how to run it.

Architecture in AWS (ca-central-1): ECS Fargate (one task: `fraud-service` + `model-service`
sidecar, X86_64) in public subnets with a public IP, RDS MySQL db.t4g.micro in private subnets
(password in Secrets Manager, port 3306 open only to the task's security group), S3 bucket for batch
job files (task role scoped to it), ECR, CloudWatch Logs, GitHub Actions deploying via an OIDC role. No NAT gateway, no load balancer in the
verification tier.

## Verified deployments

| Date | Commit | Tier | Result | `apply` | deploy workflow | `destroy` | Cost (assumptions → estimate; actual from bill) |
|---|---|---|---|---|---|---|---|
| 2026-09-16 | `4dc35b2` | verification (operator IP only, http://IP:8080) | ✅ smoke.sh 4/4 on AWS; manual login, single + batch prediction in browser | 5 min 37 s (RDS 5 min 26 s) | 2 min 52 s (build ×2, push, register, stable) | 3 min 57 s | Env existed 14:15–14:40 EDT (≈25 min). Fargate 1 vCPU/2 GB ≈15 min, RDS db.t4g.micro + 20 GB gp3 ≈18 min, 1 public IPv4, 1 secret, ≈0.8 GB in ECR → **≈ $0.02 est.**; actual: not itemised — Free-plan account, bill shows $0; credits page on 2026-09-17 still showed $0.00 used of $140 (estimates lag ~24 h) |
| 2026-09-16 | `7100f22` (first `9a6eea7`, then rolled to `7100f22` on the running service) | verification | ✅ smoke.sh 5/5 on AWS incl. async job on real S3 (task role, pre-signed download); job page with progress/preview in browser | 5 min 18 s (36 resources) | 3 min 11 s, then 3 min 05 s for the second revision | 3 min 46 s | Env existed 17:56–18:32 EDT (≈36 min). Same sizes as above plus S3 (2 objects) → **≈ $0.03 est.**; actual: see row above (same credit pool) |

Screenshots: [single prediction](images/aws-single-prediction.png),
[batch prediction](images/aws-batch-prediction.png), [async batch job page](images/aws-batch-job.png).

## Acceptance rule

- **Verification tier**: after the deploy workflow reports the service stable, the operator runs
  `scripts/smoke.sh http://<task-ip>:8080 demo demo123` from the allowed IP. Only a passing run is
  logged as verified.
- **Demo tier** (ALB + HTTPS, not built yet): the deploy workflow runs the same script against the
  public HTTPS URL and fails if it fails.

## After every destroy

Run `AWS_PROFILE=fraud scripts/aws-leftovers.sh`: it lists everything that could cost money (RDS
instances and snapshots, ECS, ECR, load balancers, NAT gateways, VPCs / ENIs / Elastic IPs / EBS /
EC2, Secrets Manager including scheduled deletions, log groups, custom IAM roles). Only the state
bucket should remain.
