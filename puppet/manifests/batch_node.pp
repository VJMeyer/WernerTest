# batch_node.pp - Standalone Batch Processor Node
# This manifest deploys only the batch processor service on dedicated nodes
# Useful for deploying batch processors closer to databases or Windows systems
# with wiskBat, separate from the application cluster.
class fileprocessing::batch_node {
  include fileprocessing::params

  $server_id = $::fileprocessing::server_id
  $cluster_servers = $::fileprocessing::cluster_servers
  $use_tls = $::fileprocessing::use_tls
  $rabbitmq_password = $::fileprocessing::rabbitmq_password

  # Install Java (required for batch processor)
  include fileprocessing::java

  # Create application directory
  file { "${fileprocessing::params::app_home}/batch-processor":
    ensure  => directory,
    owner   => $fileprocessing::params::app_user,
    group   => $fileprocessing::params::app_group,
    mode    => '0755',
    require => File[$fileprocessing::params::app_home],
  }

  # Deploy JAR file
  file { $fileprocessing::params::batch_processor_jar:
    ensure  => present,
    source  => 'puppet:///modules/fileprocessing/batch-processor.jar',
    owner   => $fileprocessing::params::app_user,
    group   => $fileprocessing::params::app_group,
    mode    => '0644',
    require => File["${fileprocessing::params::app_home}/batch-processor"],
  }

  # Create application.properties with TLS support
  file { "${fileprocessing::params::app_home}/batch-processor/application.properties":
    ensure  => file,
    content => template('fileprocessing/application-batch-tls.properties.erb'),
    owner   => $fileprocessing::params::app_user,
    group   => $fileprocessing::params::app_group,
    mode    => '0640',
    require => File["${fileprocessing::params::app_home}/batch-processor"],
    notify  => Service['batch-processor'],
  }

  # If using TLS, deploy trust store with CA certificate
  if $use_tls {
    file { "${fileprocessing::params::app_home}/batch-processor/truststore.jks":
      ensure  => present,
      source  => 'puppet:///modules/fileprocessing/rabbitmq-truststore.jks',
      owner   => $fileprocessing::params::app_user,
      group   => $fileprocessing::params::app_group,
      mode    => '0640',
      require => File["${fileprocessing::params::app_home}/batch-processor"],
      notify  => Service['batch-processor'],
    }
  }

  # Create systemd service file
  file { '/etc/systemd/system/batch-processor.service':
    ensure  => file,
    content => template('fileprocessing/batch-processor.service.erb'),
    mode    => '0644',
    notify  => Exec['systemd-reload-batch-node'],
  }

  exec { 'systemd-reload-batch-node':
    command     => '/usr/bin/systemctl daemon-reload',
    refreshonly => true,
  }

  # Enable and start batch processor service
  service { 'batch-processor':
    ensure  => running,
    enable  => true,
    require => [
      File['/etc/systemd/system/batch-processor.service'],
      File[$fileprocessing::params::batch_processor_jar],
      Class['fileprocessing::java'],
    ],
  }

  # Create log file with proper permissions
  file { "${fileprocessing::params::log_dir}/batch-processor.log":
    ensure  => file,
    owner   => $fileprocessing::params::app_user,
    group   => $fileprocessing::params::app_group,
    mode    => '0644',
    require => File[$fileprocessing::params::log_dir],
  }

  # Create directory for generated batch files
  file { "${fileprocessing::params::app_home}/batch-output":
    ensure  => directory,
    owner   => $fileprocessing::params::app_user,
    group   => $fileprocessing::params::app_group,
    mode    => '0755',
    require => File[$fileprocessing::params::app_home],
  }

  # Open outbound firewall for RabbitMQ connection
  # No inbound ports needed for batch processor
  firewalld_rich_rule { 'Allow outbound to RabbitMQ cluster':
    ensure => present,
    zone   => 'public',
    action => 'accept',
    dest   => {
      'address' => $cluster_servers[0],
    },
    port   => {
      'port'     => $use_tls ? { true => 443, default => 5672 },
      'protocol' => 'tcp',
    },
  }
}
