# init.pp - Main entry point for fileprocessing module
#
# This module orchestrates the deployment of a high-availability file processing system
# across multiple RHEL servers.
#
# Parameters:
#   $role - Server role: 'app_cluster', 'keycloak', or 'batch_node'
#   $server_id - Server identifier (1, 2, 3, 4, or 5+)
#   $cluster_servers - Array of cluster server hostnames/IPs (servers 1-3)
#   $use_tls - Enable TLS for RabbitMQ connections (default: false)
#   $enable_rabbitmq_tls - Install RabbitMQ TLS support and HAProxy (default: false)
#
# Roles:
#   - app_cluster: Full HA stack (RabbitMQ, Redis, Upload, Batch)
#   - keycloak: Authentication server
#   - batch_node: Standalone batch processor (connects to remote RabbitMQ)
#
# Example usage in site.pp:
#   node 'server1.example.com' {
#     class { 'fileprocessing':
#       role                 => 'app_cluster',
#       server_id            => 1,
#       cluster_servers      => ['server1.example.com', 'server2.example.com', 'server3.example.com'],
#       enable_rabbitmq_tls  => true,
#     }
#   }
#
#   node 'batch1.example.com' {
#     class { 'fileprocessing':
#       role            => 'batch_node',
#       server_id       => 5,
#       cluster_servers => ['server1.example.com', 'server2.example.com', 'server3.example.com'],
#       use_tls         => true,
#     }
#   }
#
class fileprocessing (
  String $role = 'app_cluster',
  Integer $server_id = 1,
  Array[String] $cluster_servers = [],
  Boolean $use_tls = false,
  Boolean $enable_rabbitmq_tls = false,
  Optional[String] $nfs_server = undef,
  Optional[String] $nfs_export = undef,
  Hash $rabbitmq_password = {},
  Hash $redis_password = {},
  Hash $keycloak_admin_password = {},
  Hash $postgres_password = {},
) {

  # Include default parameters
  include fileprocessing::params

  # Common components for all servers
  include fileprocessing::common

  # Deploy based on role
  case $role {
    'app_cluster': {
      # Servers 1-3: Application cluster with HA
      include fileprocessing::java
      include fileprocessing::docker
      include fileprocessing::rabbitmq
      include fileprocessing::redis
      include fileprocessing::upload_service
      include fileprocessing::batch_processor

      # Enable RabbitMQ TLS if requested
      if $enable_rabbitmq_tls {
        include fileprocessing::rabbitmq_tls
      }

      # Set up NFS mount if configured
      if $nfs_server and $nfs_export {
        include fileprocessing::nfs_client
      }

      # Ensure services start in correct order
      Class['fileprocessing::java']
        -> Class['fileprocessing::docker']
        -> Class['fileprocessing::rabbitmq']
        -> Class['fileprocessing::redis']
        -> Class['fileprocessing::upload_service']
        -> Class['fileprocessing::batch_processor']

      if $enable_rabbitmq_tls {
        Class['fileprocessing::rabbitmq']
          -> Class['fileprocessing::rabbitmq_tls']
      }
    }

    'keycloak': {
      # Server 4: Keycloak authentication server
      include fileprocessing::java
      include fileprocessing::docker
      include fileprocessing::keycloak

      Class['fileprocessing::java']
        -> Class['fileprocessing::docker']
        -> Class['fileprocessing::keycloak']
    }

    'batch_node': {
      # Standalone batch processor node
      # Connects to remote RabbitMQ cluster (optionally via TLS on port 443)
      # Useful for deploying near databases or Windows systems with wiskBat
      include fileprocessing::batch_node

      Class['fileprocessing::common']
        -> Class['fileprocessing::batch_node']
    }

    default: {
      fail("Unknown role: ${role}. Must be 'app_cluster', 'keycloak', or 'batch_node'")
    }
  }
}
