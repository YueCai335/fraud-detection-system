#!/usr/bin/env bash
# After `terraform destroy`: list anything still in the account that could cost money.
# Everything should print 0 / empty except the Terraform state bucket.
#
#   AWS_PROFILE=fraud scripts/aws-leftovers.sh
set -euo pipefail

q() { aws "$@" --output text 2>/dev/null || echo "?"; }

echo "RDS instances        : $(q rds describe-db-instances --query 'length(DBInstances)')"
echo "RDS snapshots        : $(q rds describe-db-snapshots --query 'length(DBSnapshots)')"
echo "ECS clusters         : $(q ecs list-clusters --query 'length(clusterArns)')"
echo "ECR repositories     : $(q ecr describe-repositories --query 'length(repositories)')"
echo "Load balancers       : $(q elbv2 describe-load-balancers --query 'length(LoadBalancers)')"
echo "NAT gateways         : $(q ec2 describe-nat-gateways --filter Name=state,Values=available,pending --query 'length(NatGateways)')"
echo "Non-default VPCs     : $(q ec2 describe-vpcs --filters Name=is-default,Values=false --query 'length(Vpcs)')"
echo "Network interfaces   : $(q ec2 describe-network-interfaces --query 'length(NetworkInterfaces)')"
echo "Elastic IPs          : $(q ec2 describe-addresses --query 'length(Addresses)')"
echo "EBS volumes          : $(q ec2 describe-volumes --query 'length(Volumes)')"
echo "EC2 instances        : $(q ec2 describe-instances --query 'length(Reservations[].Instances[])')"
echo "Secrets (+scheduled) : $(q secretsmanager list-secrets --include-planned-deletion --query 'SecretList[].Name')"
echo "Log groups           : $(q logs describe-log-groups --query 'logGroups[].logGroupName')"
echo "Custom IAM roles     : $(q iam list-roles --query "Roles[?starts_with(RoleName, 'fraud')].RoleName")"
echo "S3 buckets           : $(aws s3 ls 2>/dev/null | awk '{print $3}' | tr '\n' ' ')"
echo
echo "Expected: only the fraud-detection-tfstate-* bucket remains."
