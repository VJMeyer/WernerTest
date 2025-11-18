# keycloak.pp - Deploy and configure Keycloak with PostgreSQL
class fileprocessing::keycloak {
  include fileprocessing::params

  $keycloak_admin_password = $::fileprocessing::keycloak_admin_password
  $postgres_password = $::fileprocessing::postgres_password

  # Create Keycloak directory structure
  file { ["${fileprocessing::params::app_home}/keycloak",
          "${fileprocessing::params::app_home}/keycloak/data",
          "${fileprocessing::params::app_home}/keycloak/postgres-data"]:
    ensure  => directory,
    owner   => $fileprocessing::params::app_user,
    group   => $fileprocessing::params::app_group,
    mode    => '0755',
    require => File[$fileprocessing::params::app_home],
  }

  # Copy Keycloak realm configuration
  file { "${fileprocessing::params::app_home}/keycloak/realm-export.json":
    ensure  => file,
    source  => 'puppet:///modules/fileprocessing/realm-export.json',
    owner   => $fileprocessing::params::app_user,
    group   => $fileprocessing::params::app_group,
    mode    => '0644',
    require => File["${fileprocessing::params::app_home}/keycloak"],
  }

  # Create docker-compose file for Keycloak with PostgreSQL
  file { "${fileprocessing::params::app_home}/keycloak/docker-compose.yml":
    ensure  => file,
    content => template('fileprocessing/keycloak-docker-compose.yml.erb'),
    owner   => $fileprocessing::params::app_user,
    group   => $fileprocessing::params::app_group,
    mode    => '0644',
    require => File["${fileprocessing::params::app_home}/keycloak"],
    notify  => Exec['restart-keycloak'],
  }

  # Start Keycloak using docker-compose
  exec { 'start-keycloak':
    command => "/usr/local/bin/docker-compose -f ${fileprocessing::params::app_home}/keycloak/docker-compose.yml up -d",
    cwd     => "${fileprocessing::params::app_home}/keycloak",
    unless  => "/usr/local/bin/docker-compose -f ${fileprocessing::params::app_home}/keycloak/docker-compose.yml ps | /bin/grep -q keycloak",
    require => [
      Class['fileprocessing::docker'],
      File["${fileprocessing::params::app_home}/keycloak/docker-compose.yml"],
      File["${fileprocessing::params::app_home}/keycloak/realm-export.json"],
    ],
  }

  exec { 'restart-keycloak':
    command     => "/usr/local/bin/docker-compose -f ${fileprocessing::params::app_home}/keycloak/docker-compose.yml restart",
    cwd         => "${fileprocessing::params::app_home}/keycloak",
    refreshonly => true,
    require     => Exec['start-keycloak'],
  }

  # Create systemd service for Keycloak (to auto-start on boot)
  file { '/etc/systemd/system/keycloak.service':
    ensure  => file,
    content => template('fileprocessing/keycloak.service.erb'),
    mode    => '0644',
    notify  => Exec['systemd-reload-keycloak'],
  }

  exec { 'systemd-reload-keycloak':
    command     => '/usr/bin/systemctl daemon-reload',
    refreshonly => true,
  }

  service { 'keycloak':
    ensure  => running,
    enable  => true,
    require => [
      File['/etc/systemd/system/keycloak.service'],
      Exec['start-keycloak'],
    ],
  }

  # Open firewall ports for Keycloak
  firewalld_port { 'Allow Keycloak HTTP':
    ensure   => present,
    zone     => 'public',
    port     => $fileprocessing::params::keycloak_port,
    protocol => 'tcp',
  }

  firewalld_port { 'Allow Keycloak HTTPS':
    ensure   => present,
    zone     => 'public',
    port     => 8543,
    protocol => 'tcp',
  }
}
