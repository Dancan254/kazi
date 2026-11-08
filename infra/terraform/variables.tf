variable "aws_region" {
  description = "AWS region for all resources"
  type        = string
  default     = "eu-west-1"
}

variable "environment" {
  description = "Lowercase environment name used in resource names"
  type        = string
  default     = "dev"

  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.environment))
    error_message = "Use only lowercase letters, numbers, and hyphens."
  }
}

variable "repository_url" {
  description = "Public HTTPS Git URL cloned onto the EC2 instance"
  type        = string
}

variable "repository_branch" {
  description = "Git branch deployed on first boot"
  type        = string
  default     = "main"
}

variable "instance_type" {
  description = "EC2 size; 4 GB RAM is the recommended minimum"
  type        = string
  default     = "t3.medium"
}

variable "root_volume_size_gb" {
  description = "Encrypted gp3 root volume size"
  type        = number
  default     = 30
}

variable "backup_retention_days" {
  description = "Days before current and noncurrent backup versions expire"
  type        = number
  default     = 30
}

variable "monthly_budget_usd" {
  description = "Account-wide monthly cost budget used for the 80 percent alert"
  type        = number
  default     = 30
}

variable "budget_email" {
  description = "Email address that receives the AWS Budget alert"
  type        = string
}
