resource "aws_db_subnet_group" "main" {
  name       = "${var.name}-db"
  subnet_ids = aws_subnet.private[*].id
}

# Demo-sized, single-AZ, no public access. Data is re-creatable (Flyway + demo seeder),
# so no final snapshot and no backups: destroy must leave nothing that costs money.
resource "aws_db_instance" "mysql" {
  identifier                  = "${var.name}-mysql"
  engine                      = "mysql"
  engine_version              = "8.4.11"
  instance_class              = "db.t4g.micro"
  allocated_storage           = 20
  storage_type                = "gp3"
  db_name                     = "fraud"
  username                    = "fraud"
  manage_master_user_password = true # password lives in Secrets Manager, never in state or CI
  db_subnet_group_name        = aws_db_subnet_group.main.name
  vpc_security_group_ids      = [aws_security_group.rds.id]
  publicly_accessible         = false
  multi_az                    = false
  backup_retention_period     = 0
  skip_final_snapshot         = true
  deletion_protection         = false
  apply_immediately           = true
}

locals {
  db_secret_arn = aws_db_instance.mysql.master_user_secret[0].secret_arn
  db_url        = "jdbc:mysql://${aws_db_instance.mysql.address}:3306/fraud?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC"
}
