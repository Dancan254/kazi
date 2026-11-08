output "application_url" {
  description = "Base URL for REST requests"
  value       = "https://${aws_eip.app.public_ip}.sslip.io"
}

output "mcp_url" {
  description = "Streamable HTTP MCP endpoint"
  value       = "https://${aws_eip.app.public_ip}.sslip.io/mcp"
}

output "instance_id" {
  description = "EC2 instance ID used with AWS Systems Manager"
  value       = aws_instance.app.id
}

output "backup_bucket_name" {
  description = "Private S3 bucket receiving daily PostgreSQL backups"
  value       = aws_s3_bucket.backups.id
}

output "mcp_api_key_parameter" {
  description = "SecureString parameter containing the generated bootstrap admin API key"
  value       = "/kazi/${var.environment}/mcp-api-key"
}
