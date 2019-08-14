output "proxy_grpc_server_endpoint" {
  value = "haystack-agent:${var.blobs_service_port}"
}