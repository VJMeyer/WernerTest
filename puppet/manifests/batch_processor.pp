# batch_processor.pp - Deploy and configure Batch Processor
class fileprocessing::batch_processor {
  include fileprocessing::params

  $server_id = $::fileprocessing::server_id

  # Create application directory
  file { "${fileprocessing::params::app_home}/batch-processor":
    ensure  => directory,
    owner   => $fileprocessing::params::app_user,
    group   => $fileprocessing::params::app_group,
    mode    => '0755',
    require => File[$fileprocessing::params::app_home],
  }

  # Deploy JAR file (assumes it's been built and is available)
  file { $fileprocessing::params::batch_processor_jar:
    ensure  => present,
    source  => 'puppet:///modules/fileprocessing/batch-processor.jar',  # You'll need to provide this
    owner   => $fileprocessing::params::app_user,
    group   => $fileprocessing::params::app_group,
    mode    => '0644',
    require => File["${fileprocessing::params::app_home}/batch-processor"],
  }

  # Create application.properties
  file { "${fileprocessing::params::app_home}/batch-processor/application.properties":
    ensure  => file,
    content => template('fileprocessing/application-batch.properties.erb'),
    owner   => $fileprocessing::params::app_user,
    group   => $fileprocessing::params::app_group,
    mode    => '0640',
    require => File["${fileprocessing::params::app_home}/batch-processor"],
    notify  => Service['batch-processor'],
  }

  # Create systemd service file
  file { '/etc/systemd/system/batch-processor.service':
    ensure  => file,
    content => template('fileprocessing/batch-processor.service.erb'),
    mode    => '0644',
    notify  => Exec['systemd-reload-batch'],
  }

  exec { 'systemd-reload-batch':
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
      Class['fileprocessing::rabbitmq'],
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
}
