# rabbitmq.pp - Install and configure RabbitMQ cluster
class fileprocessing::rabbitmq {
  include fileprocessing::params

  $server_id = $::fileprocessing::server_id
  $cluster_servers = $::fileprocessing::cluster_servers
  $rabbitmq_password = $::fileprocessing::rabbitmq_password

  # Install Erlang (required for RabbitMQ)
  package { 'erlang':
    ensure => installed,
  }

  # Add RabbitMQ repository
  exec { 'add_rabbitmq_repo':
    command => '/usr/bin/curl -s https://packagecloud.io/install/repositories/rabbitmq/rabbitmq-server/script.rpm.sh | /bin/bash',
    creates => '/etc/yum.repos.d/rabbitmq_rabbitmq-server.repo',
    require => Package['curl'],
  }

  # Install RabbitMQ
  package { 'rabbitmq-server':
    ensure  => installed,
    require => [Package['erlang'], Exec['add_rabbitmq_repo']],
  }

  # Configure RabbitMQ
  file { '/etc/rabbitmq/rabbitmq.conf':
    ensure  => file,
    content => template('fileprocessing/rabbitmq.conf.erb'),
    mode    => '0644',
    owner   => 'rabbitmq',
    group   => 'rabbitmq',
    require => Package['rabbitmq-server'],
    notify  => Service['rabbitmq-server'],
  }

  # Set up Erlang cookie for clustering (must be same on all nodes)
  file { '/var/lib/rabbitmq/.erlang.cookie':
    ensure  => file,
    content => 'FILEPROCESSINGCLUSTERCOOKIE',  # Change this in production!
    mode    => '0400',
    owner   => 'rabbitmq',
    group   => 'rabbitmq',
    require => Package['rabbitmq-server'],
    notify  => Service['rabbitmq-server'],
  }

  # Enable and start RabbitMQ
  service { 'rabbitmq-server':
    ensure  => running,
    enable  => true,
    require => Package['rabbitmq-server'],
  }

  # Enable RabbitMQ management plugin
  exec { 'enable_rabbitmq_management':
    command => '/usr/sbin/rabbitmq-plugins enable rabbitmq_management',
    unless  => '/usr/sbin/rabbitmq-plugins list -e | /bin/grep -q rabbitmq_management',
    require => Service['rabbitmq-server'],
    notify  => Service['rabbitmq-server'],
  }

  # Open firewall ports for RabbitMQ
  firewalld_port { 'Allow RabbitMQ AMQP':
    ensure   => present,
    zone     => 'public',
    port     => $fileprocessing::params::rabbitmq_port,
    protocol => 'tcp',
  }

  firewalld_port { 'Allow RabbitMQ Management':
    ensure   => present,
    zone     => 'public',
    port     => $fileprocessing::params::rabbitmq_management_port,
    protocol => 'tcp',
  }

  firewalld_port { 'Allow RabbitMQ Clustering':
    ensure   => present,
    zone     => 'public',
    port     => 25672,
    protocol => 'tcp',
  }

  firewalld_port { 'Allow Erlang Port Mapper':
    ensure   => present,
    zone     => 'public',
    port     => 4369,
    protocol => 'tcp',
  }

  # Configure RabbitMQ cluster (only after service is running)
  if $server_id == 1 {
    # Server 1 is the master node
    exec { 'create_rabbitmq_vhost':
      command => "/usr/sbin/rabbitmqctl add_vhost ${fileprocessing::params::rabbitmq_vhost}",
      unless  => "/usr/sbin/rabbitmqctl list_vhosts | /bin/grep -q ${fileprocessing::params::rabbitmq_vhost}",
      require => Service['rabbitmq-server'],
    }

    exec { 'create_rabbitmq_user':
      command => "/usr/sbin/rabbitmqctl add_user ${fileprocessing::params::rabbitmq_user} '${rabbitmq_password['password']}'",
      unless  => "/usr/sbin/rabbitmqctl list_users | /bin/grep -q ${fileprocessing::params::rabbitmq_user}",
      require => Exec['create_rabbitmq_vhost'],
    }

    exec { 'set_rabbitmq_permissions':
      command => "/usr/sbin/rabbitmqctl set_permissions -p ${fileprocessing::params::rabbitmq_vhost} ${fileprocessing::params::rabbitmq_user} '.*' '.*' '.*'",
      require => Exec['create_rabbitmq_user'],
    }

    exec { 'set_rabbitmq_user_tags':
      command => "/usr/sbin/rabbitmqctl set_user_tags ${fileprocessing::params::rabbitmq_user} administrator",
      require => Exec['create_rabbitmq_user'],
    }

    # Set HA policy for queues
    exec { 'set_ha_policy':
      command => "/usr/sbin/rabbitmqctl set_policy -p ${fileprocessing::params::rabbitmq_vhost} ha-all '.*' '{\"ha-mode\":\"all\",\"ha-sync-mode\":\"automatic\"}'",
      require => Exec['create_rabbitmq_vhost'],
    }
  } else {
    # Servers 2 and 3 join the cluster
    $master_server = $cluster_servers[0]

    exec { 'join_rabbitmq_cluster':
      command => "/usr/sbin/rabbitmqctl stop_app && /usr/sbin/rabbitmqctl join_cluster rabbit@${master_server} && /usr/sbin/rabbitmqctl start_app",
      unless  => "/usr/sbin/rabbitmqctl cluster_status | /bin/grep -q ${master_server}",
      require => [Service['rabbitmq-server'], File['/var/lib/rabbitmq/.erlang.cookie']],
    }
  }
}
