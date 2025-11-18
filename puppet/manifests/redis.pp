# redis.pp - Install and configure Redis with Sentinel for HA
class fileprocessing::redis {
  include fileprocessing::params

  $server_id = $::fileprocessing::server_id
  $cluster_servers = $::fileprocessing::cluster_servers
  $redis_password = $::fileprocessing::redis_password

  # Install Redis
  package { 'redis':
    ensure => installed,
  }

  # Determine Redis role based on server_id
  $is_master = ($server_id == 1)
  $master_ip = $cluster_servers[0]

  # Configure Redis
  file { '/etc/redis/redis.conf':
    ensure  => file,
    content => template('fileprocessing/redis.conf.erb'),
    mode    => '0640',
    owner   => 'redis',
    group   => 'redis',
    require => Package['redis'],
    notify  => Service['redis'],
  }

  # Configure Redis Sentinel
  file { '/etc/redis/sentinel.conf':
    ensure  => file,
    content => template('fileprocessing/redis-sentinel.conf.erb'),
    mode    => '0640',
    owner   => 'redis',
    group   => 'redis',
    require => Package['redis'],
    notify  => Service['redis-sentinel'],
  }

  # Enable and start Redis service
  service { 'redis':
    ensure  => running,
    enable  => true,
    require => [Package['redis'], File['/etc/redis/redis.conf']],
  }

  # Enable and start Redis Sentinel service
  service { 'redis-sentinel':
    ensure  => running,
    enable  => true,
    require => [Package['redis'], File['/etc/redis/sentinel.conf'], Service['redis']],
  }

  # Open firewall ports for Redis
  firewalld_port { 'Allow Redis':
    ensure   => present,
    zone     => 'public',
    port     => $fileprocessing::params::redis_port,
    protocol => 'tcp',
  }

  firewalld_port { 'Allow Redis Sentinel':
    ensure   => present,
    zone     => 'public',
    port     => $fileprocessing::params::redis_sentinel_port,
    protocol => 'tcp',
  }

  # Create Redis data directory with proper permissions
  file { '/var/lib/redis':
    ensure  => directory,
    owner   => 'redis',
    group   => 'redis',
    mode    => '0755',
    require => Package['redis'],
  }

  # Create Redis log directory
  file { '/var/log/redis':
    ensure  => directory,
    owner   => 'redis',
    group   => 'redis',
    mode    => '0755',
    require => Package['redis'],
  }
}
