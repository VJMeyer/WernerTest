# nfs_client.pp - Configure NFS client for shared storage
class fileprocessing::nfs_client {
  include fileprocessing::params

  $nfs_server = $::fileprocessing::nfs_server
  $nfs_export = $::fileprocessing::nfs_export

  # Install NFS utilities
  package { 'nfs-utils':
    ensure => installed,
  }

  # Create NFS mount point
  file { $fileprocessing::params::nfs_mount_point:
    ensure  => directory,
    owner   => $fileprocessing::params::app_user,
    group   => $fileprocessing::params::app_group,
    mode    => '0755',
    require => User[$fileprocessing::params::app_user],
  }

  # Mount NFS share
  mount { $fileprocessing::params::nfs_mount_point:
    ensure  => mounted,
    device  => "${nfs_server}:${nfs_export}",
    fstype  => 'nfs',
    options => 'defaults,_netdev',
    atboot  => true,
    require => [
      Package['nfs-utils'],
      File[$fileprocessing::params::nfs_mount_point],
    ],
  }

  # Create symlink from upload directory to NFS mount
  file { $fileprocessing::params::upload_dir:
    ensure  => link,
    target  => "${fileprocessing::params::nfs_mount_point}/uploads",
    force   => true,
    require => Mount[$fileprocessing::params::nfs_mount_point],
  }

  # Ensure uploads directory exists on NFS
  exec { 'create_nfs_uploads_dir':
    command => "/bin/mkdir -p ${fileprocessing::params::nfs_mount_point}/uploads && /bin/chown ${fileprocessing::params::app_user}:${fileprocessing::params::app_group} ${fileprocessing::params::nfs_mount_point}/uploads",
    unless  => "/bin/test -d ${fileprocessing::params::nfs_mount_point}/uploads",
    require => Mount[$fileprocessing::params::nfs_mount_point],
  }
}
