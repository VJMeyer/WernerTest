# params.pp - Default parameters for the file processing system
class fileprocessing::params {
  # Java configuration
  $java_version = '21'
  $java_package = 'java-21-openjdk-devel'

  # Application configuration
  $app_user = 'fileprocessing'
  $app_group = 'fileprocessing'
  $app_home = '/opt/fileprocessing'
  $upload_dir = '/var/lib/fileprocessing/uploads'
  $log_dir = '/var/log/fileprocessing'

  # RabbitMQ configuration
  $rabbitmq_version = '3.12'
  $rabbitmq_port = 5672
  $rabbitmq_management_port = 15672
  $rabbitmq_user = 'fileprocessing'
  $rabbitmq_vhost = '/fileprocessing'
  $rabbitmq_cluster_name = 'fileprocessing-cluster'

  # Redis configuration
  $redis_version = '7.0'
  $redis_port = 6379
  $redis_sentinel_port = 26379
  $redis_master_name = 'mymaster'

  # Upload Service configuration
  $upload_service_port = 8080
  $upload_service_jar = "${app_home}/upload-service/upload-service.jar"

  # Batch Processor configuration
  $batch_processor_jar = "${app_home}/batch-processor/batch-processor.jar"

  # Keycloak configuration
  $keycloak_version = '23.0'
  $keycloak_port = 8180
  $keycloak_admin_user = 'admin'
  $keycloak_db_vendor = 'postgres'

  # Network configuration
  $nfs_mount_point = '/mnt/fileprocessing-nfs'
  $nfs_server = undef  # Must be set in Hiera

  # Maven configuration
  $maven_version = '3.9.5'
  $maven_home = '/opt/maven'
}
