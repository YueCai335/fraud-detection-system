variable "region" {
  type    = string
  default = "ca-central-1"
}

variable "name" {
  description = "Prefix for every resource name."
  type        = string
  default     = "fraud"
}

variable "allowed_cidr" {
  description = "Verification tier: the only CIDR allowed to reach the service on :8080 (your IP/32)."
  type        = string
}

variable "github_repo" {
  description = "owner/repo allowed to assume the deploy role via GitHub OIDC."
  type        = string
  default     = "YueCai335/fraud-detection-system"
}

variable "spring_profiles" {
  description = "SPRING_PROFILES_ACTIVE for fraud-service. 'demo' seeds the demo account."
  type        = string
  default     = "demo"
}

variable "task_cpu" {
  type    = number
  default = 1024 # 1 vCPU for the whole task (both containers)
}

variable "task_memory" {
  type    = number
  default = 2048 # MiB
}

variable "enable_alb" {
  description = "Demo tier: put an HTTPS ALB in front of the service. Not implemented yet."
  type        = bool
  default     = false
}
