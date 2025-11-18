# docker.pp - Install and configure Docker
class fileprocessing::docker {
  include fileprocessing::params

  # Install Docker prerequisites
  package { ['yum-utils', 'device-mapper-persistent-data', 'lvm2']:
    ensure => installed,
  }

  # Add Docker repository
  exec { 'add_docker_repo':
    command => '/usr/bin/yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo',
    creates => '/etc/yum.repos.d/docker-ce.repo',
    require => Package['yum-utils'],
  }

  # Install Docker
  package { ['docker-ce', 'docker-ce-cli', 'containerd.io', 'docker-compose-plugin']:
    ensure  => installed,
    require => Exec['add_docker_repo'],
  }

  # Enable and start Docker service
  service { 'docker':
    ensure  => running,
    enable  => true,
    require => Package['docker-ce'],
  }

  # Add application user to docker group
  exec { 'add_user_to_docker_group':
    command => "/usr/sbin/usermod -aG docker ${fileprocessing::params::app_user}",
    unless  => "/usr/bin/groups ${fileprocessing::params::app_user} | /bin/grep -q docker",
    require => [
      Package['docker-ce'],
      User[$fileprocessing::params::app_user],
    ],
  }

  # Install Docker Compose (standalone)
  exec { 'install_docker_compose':
    command => '/usr/bin/curl -L "https://github.com/docker/compose/releases/download/v2.23.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose && /bin/chmod +x /usr/local/bin/docker-compose',
    creates => '/usr/local/bin/docker-compose',
    require => Package['curl'],
  }

  # Configure Docker daemon
  file { '/etc/docker/daemon.json':
    ensure  => file,
    content => template('fileprocessing/docker-daemon.json.erb'),
    mode    => '0644',
    notify  => Service['docker'],
    require => Package['docker-ce'],
  }
}
