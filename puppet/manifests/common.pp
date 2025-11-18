# common.pp - Common configuration for all servers
class fileprocessing::common {
  include fileprocessing::params

  # Ensure firewalld is installed and running
  package { 'firewalld':
    ensure => installed,
  }

  service { 'firewalld':
    ensure  => running,
    enable  => true,
    require => Package['firewalld'],
  }

  # Install common utilities
  package { ['wget', 'curl', 'unzip', 'tar', 'git', 'vim', 'net-tools']:
    ensure => installed,
  }

  # Create application user and group
  group { $fileprocessing::params::app_group:
    ensure => present,
    system => true,
  }

  user { $fileprocessing::params::app_user:
    ensure     => present,
    gid        => $fileprocessing::params::app_group,
    home       => $fileprocessing::params::app_home,
    managehome => true,
    system     => true,
    shell      => '/bin/bash',
    require    => Group[$fileprocessing::params::app_group],
  }

  # Create application directories
  file { [$fileprocessing::params::app_home,
          $fileprocessing::params::log_dir]:
    ensure  => directory,
    owner   => $fileprocessing::params::app_user,
    group   => $fileprocessing::params::app_group,
    mode    => '0755',
    require => User[$fileprocessing::params::app_user],
  }

  # Configure system limits for the application user
  file { '/etc/security/limits.d/fileprocessing.conf':
    ensure  => file,
    content => template('fileprocessing/limits.conf.erb'),
    mode    => '0644',
  }

  # Disable SELinux for easier initial setup (can be re-enabled with proper policies)
  exec { 'disable_selinux':
    command => '/usr/sbin/setenforce 0',
    unless  => '/usr/sbin/getenforce | /bin/grep -i permissive',
    path    => ['/usr/bin', '/usr/sbin', '/bin', '/sbin'],
  }

  file { '/etc/selinux/config':
    ensure  => file,
    content => "SELINUX=permissive\nSELINUXTYPE=targeted\n",
    mode    => '0644',
  }
}
