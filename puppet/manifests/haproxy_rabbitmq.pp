# haproxy_rabbitmq.pp - HAProxy for RabbitMQ TLS termination on port 443
# Terminates TLS on port 443 and forwards to RabbitMQ on port 5672
# Allows batch processors to connect via standard HTTPS port
class fileprocessing::haproxy_rabbitmq {
  include fileprocessing::params

  $cluster_servers = $::fileprocessing::cluster_servers

  # Install HAProxy
  package { 'haproxy':
    ensure => installed,
  }

  # Create HAProxy TLS certificate directory
  file { '/etc/haproxy/tls':
    ensure  => directory,
    owner   => 'haproxy',
    group   => 'haproxy',
    mode    => '0750',
    require => Package['haproxy'],
  }

  # Deploy combined certificate (cert + key) for HAProxy
  file { '/etc/haproxy/tls/rabbitmq.pem':
    ensure  => present,
    source  => 'puppet:///modules/fileprocessing/haproxy-rabbitmq.pem',
    owner   => 'haproxy',
    group   => 'haproxy',
    mode    => '0600',
    require => File['/etc/haproxy/tls'],
    notify  => Service['haproxy'],
  }

  # Configure HAProxy for RabbitMQ TLS termination
  file { '/etc/haproxy/haproxy.cfg':
    ensure  => file,
    content => template('fileprocessing/haproxy-rabbitmq.cfg.erb'),
    owner   => 'root',
    group   => 'root',
    mode    => '0644',
    require => Package['haproxy'],
    notify  => Service['haproxy'],
  }

  # Enable and start HAProxy
  service { 'haproxy':
    ensure  => running,
    enable  => true,
    require => [
      Package['haproxy'],
      File['/etc/haproxy/haproxy.cfg'],
    ],
  }

  # Open firewall port 443 for TLS connections
  firewalld_port { 'Allow RabbitMQ via HTTPS (443)':
    ensure   => present,
    zone     => 'public',
    port     => 443,
    protocol => 'tcp',
  }

  # Configure SELinux to allow HAProxy to bind to port 443
  exec { 'selinux_haproxy_port_443':
    command => '/usr/sbin/semanage port -a -t http_port_t -p tcp 443 || true',
    unless  => '/usr/sbin/semanage port -l | /bin/grep -q "http_port_t.*443"',
    path    => ['/usr/bin', '/usr/sbin', '/bin', '/sbin'],
    require => Package['haproxy'],
  }

  # Allow HAProxy to connect to RabbitMQ backend
  exec { 'selinux_haproxy_connect':
    command => '/usr/sbin/setsebool -P haproxy_connect_any 1',
    unless  => '/usr/sbin/getsebool haproxy_connect_any | /bin/grep -q "on"',
    path    => ['/usr/bin', '/usr/sbin', '/bin', '/sbin'],
    require => Package['haproxy'],
  }
}
