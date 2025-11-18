# rabbitmq_tls.pp - Configure RabbitMQ with TLS/SSL support
# Enables both native AMQPS (port 5671) and HAProxy termination (port 443)
class fileprocessing::rabbitmq_tls {
  include fileprocessing::params

  $server_id = $::fileprocessing::server_id
  $cluster_servers = $::fileprocessing::cluster_servers

  # Create TLS certificate directory
  file { '/etc/rabbitmq/tls':
    ensure  => directory,
    owner   => 'rabbitmq',
    group   => 'rabbitmq',
    mode    => '0750',
    require => Package['rabbitmq-server'],
  }

  # Deploy server certificate
  file { '/etc/rabbitmq/tls/server-cert.pem':
    ensure  => present,
    source  => 'puppet:///modules/fileprocessing/rabbitmq-server-cert.pem',
    owner   => 'rabbitmq',
    group   => 'rabbitmq',
    mode    => '0644',
    require => File['/etc/rabbitmq/tls'],
    notify  => Service['rabbitmq-server'],
  }

  # Deploy server private key
  file { '/etc/rabbitmq/tls/server-key.pem':
    ensure  => present,
    source  => 'puppet:///modules/fileprocessing/rabbitmq-server-key.pem',
    owner   => 'rabbitmq',
    group   => 'rabbitmq',
    mode    => '0600',
    require => File['/etc/rabbitmq/tls'],
    notify  => Service['rabbitmq-server'],
  }

  # Deploy CA certificate
  file { '/etc/rabbitmq/tls/ca-cert.pem':
    ensure  => present,
    source  => 'puppet:///modules/fileprocessing/rabbitmq-ca-cert.pem',
    owner   => 'rabbitmq',
    group   => 'rabbitmq',
    mode    => '0644',
    require => File['/etc/rabbitmq/tls'],
    notify  => Service['rabbitmq-server'],
  }

  # Configure RabbitMQ for TLS
  file { '/etc/rabbitmq/rabbitmq-tls.conf':
    ensure  => file,
    content => template('fileprocessing/rabbitmq-tls.conf.erb'),
    mode    => '0644',
    owner   => 'rabbitmq',
    group   => 'rabbitmq',
    require => Package['rabbitmq-server'],
    notify  => Service['rabbitmq-server'],
  }

  # Append TLS configuration to main config
  exec { 'enable_rabbitmq_tls_config':
    command => "/bin/bash -c 'cat /etc/rabbitmq/rabbitmq-tls.conf >> /etc/rabbitmq/rabbitmq.conf'",
    unless  => '/bin/grep -q "listeners.ssl.default" /etc/rabbitmq/rabbitmq.conf',
    require => [
      File['/etc/rabbitmq/rabbitmq.conf'],
      File['/etc/rabbitmq/rabbitmq-tls.conf'],
    ],
    notify  => Service['rabbitmq-server'],
  }

  # Open firewall port for AMQPS (5671)
  firewalld_port { 'Allow RabbitMQ AMQPS':
    ensure   => present,
    zone     => 'public',
    port     => 5671,
    protocol => 'tcp',
  }

  # Install HAProxy for TLS termination on port 443
  include fileprocessing::haproxy_rabbitmq
}
