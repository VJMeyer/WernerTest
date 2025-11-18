# upload_service.pp - Deploy and configure Upload Service
class fileprocessing::upload_service {
  include fileprocessing::params

  $server_id = $::fileprocessing::server_id
  $cluster_servers = $::fileprocessing::cluster_servers

  # Install Maven for building the application
  exec { 'download_maven':
    command => "/usr/bin/wget https://archive.apache.org/dist/maven/maven-3/${fileprocessing::params::maven_version}/binaries/apache-maven-${fileprocessing::params::maven_version}-bin.tar.gz -O /tmp/maven.tar.gz",
    creates => '/tmp/maven.tar.gz',
    require => Package['wget'],
  }

  exec { 'extract_maven':
    command => "/bin/tar xzf /tmp/maven.tar.gz -C /opt && /bin/ln -sf /opt/apache-maven-${fileprocessing::params::maven_version} ${fileprocessing::params::maven_home}",
    creates => $fileprocessing::params::maven_home,
    require => Exec['download_maven'],
  }

  file { '/etc/profile.d/maven.sh':
    ensure  => file,
    content => "export M2_HOME=${fileprocessing::params::maven_home}\nexport PATH=\$M2_HOME/bin:\$PATH\n",
    mode    => '0644',
    require => Exec['extract_maven'],
  }

  # Create application directory
  file { "${fileprocessing::params::app_home}/upload-service":
    ensure  => directory,
    owner   => $fileprocessing::params::app_user,
    group   => $fileprocessing::params::app_group,
    mode    => '0755',
    require => File[$fileprocessing::params::app_home],
  }

  # Create upload directory
  file { $fileprocessing::params::upload_dir:
    ensure  => directory,
    owner   => $fileprocessing::params::app_user,
    group   => $fileprocessing::params::app_group,
    mode    => '0755',
    require => User[$fileprocessing::params::app_user],
  }

  # Deploy JAR file (assumes it's been built and is available)
  # In production, you'd copy this from a artifact repository or NFS share
  file { $fileprocessing::params::upload_service_jar:
    ensure  => present,
    source  => 'puppet:///modules/fileprocessing/upload-service.jar',  # You'll need to provide this
    owner   => $fileprocessing::params::app_user,
    group   => $fileprocessing::params::app_group,
    mode    => '0644',
    require => File["${fileprocessing::params::app_home}/upload-service"],
  }

  # Create application.properties
  file { "${fileprocessing::params::app_home}/upload-service/application.properties":
    ensure  => file,
    content => template('fileprocessing/application-upload.properties.erb'),
    owner   => $fileprocessing::params::app_user,
    group   => $fileprocessing::params::app_group,
    mode    => '0640',
    require => File["${fileprocessing::params::app_home}/upload-service"],
    notify  => Service['upload-service'],
  }

  # Create systemd service file
  file { '/etc/systemd/system/upload-service.service':
    ensure  => file,
    content => template('fileprocessing/upload-service.service.erb'),
    mode    => '0644',
    notify  => Exec['systemd-reload-upload'],
  }

  exec { 'systemd-reload-upload':
    command     => '/usr/bin/systemctl daemon-reload',
    refreshonly => true,
  }

  # Enable and start upload service
  service { 'upload-service':
    ensure  => running,
    enable  => true,
    require => [
      File['/etc/systemd/system/upload-service.service'],
      File[$fileprocessing::params::upload_service_jar],
      Class['fileprocessing::java'],
      Class['fileprocessing::rabbitmq'],
      Class['fileprocessing::redis'],
    ],
  }

  # Open firewall port for upload service
  firewalld_port { 'Allow Upload Service':
    ensure   => present,
    zone     => 'public',
    port     => $fileprocessing::params::upload_service_port,
    protocol => 'tcp',
  }

  # Create log file with proper permissions
  file { "${fileprocessing::params::log_dir}/upload-service.log":
    ensure  => file,
    owner   => $fileprocessing::params::app_user,
    group   => $fileprocessing::params::app_group,
    mode    => '0644',
    require => File[$fileprocessing::params::log_dir],
  }
}
