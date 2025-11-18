# init.pp - Main entry point for fileprocessing module
#
# This module orchestrates the deployment of a high-availability file processing system
# across multiple RHEL servers.
#
# Parameters:
#   $role - Server role: 'app_cluster' (servers 1-3) or 'keycloak' (server 4)
#   $server_id - Server identifier (1, 2, 3, or 4)
#   $cluster_servers - Array of cluster server hostnames/IPs (servers 1-3)
#
# Example usage in site.pp:
#   node 'server1.example.com' {
#     class { 'fileprocessing':
#       role            => 'app_cluster',
#       server_id       => 1,
#       cluster_servers => ['server1.example.com', 'server2.example.com', 'server3.example.com'],
#     }
#   }
#
class fileprocessing (
  String $role = 'app_cluster',
  Integer $server_id = 1,
  Array[String] $cluster_servers = [],
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

    default: {
      fail("Unknown role: ${role}. Must be 'app_cluster' or 'keycloak'")
    }
  }
}
