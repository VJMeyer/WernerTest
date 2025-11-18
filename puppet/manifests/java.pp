# java.pp - Install and configure Java 21
class fileprocessing::java {
  include fileprocessing::params

  # Install Java 21 (OpenJDK)
  package { $fileprocessing::params::java_package:
    ensure => installed,
  }

  # Set JAVA_HOME environment variable
  file { '/etc/profile.d/java.sh':
    ensure  => file,
    content => "export JAVA_HOME=/usr/lib/jvm/java-${fileprocessing::params::java_version}-openjdk\nexport PATH=\$JAVA_HOME/bin:\$PATH\n",
    mode    => '0644',
    require => Package[$fileprocessing::params::java_package],
  }

  # Verify Java installation
  exec { 'verify_java':
    command => '/usr/bin/java -version',
    require => Package[$fileprocessing::params::java_package],
    unless  => '/usr/bin/java -version 2>&1 | /bin/grep -q "openjdk version"',
  }
}
