# File Processing System - Puppet Deployment Guide

This Puppet module deploys a highly available file processing system across 4 RHEL servers in a VMware cluster.

## Architecture Overview

### Server Layout

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       VMware Cluster (RHEL Servers)                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                  │
│  │  Server 1    │  │  Server 2    │  │  Server 3    │                  │
│  ├──────────────┤  ├──────────────┤  ├──────────────┤                  │
│  │ RabbitMQ (M) │  │ RabbitMQ     │  │ RabbitMQ     │  Application     │
│  │ Redis (M)    │  │ Redis (R)    │  │ Redis (R)    │  Cluster         │
│  │ Redis Sent.  │  │ Redis Sent.  │  │ Redis Sent.  │  (HA)            │
│  │ Upload Svc   │  │ Upload Svc   │  │ Upload Svc   │                  │
│  │ Batch Proc   │  │ Batch Proc   │  │ Batch Proc   │                  │
│  └──────────────┘  └──────────────┘  └──────────────┘                  │
│                                                                         │
│  ┌──────────────────────────────────┐                                   │
│  │         Server 4                 │                                   │
│  ├──────────────────────────────────┤                                   │
│  │  Keycloak (Docker)               │  Authentication                   │
│  │  PostgreSQL (Docker)             │  Server                           │
│  └──────────────────────────────────┘                                   │
│                                                                         │
│  Optional:                                                              │
│  ┌──────────────────────────────────┐                                   │
│  │   NFS Server (Shared Storage)    │                                   │
│  └──────────────────────────────────┘                                   │
└─────────────────────────────────────────────────────────────────────────┘
```

### High Availability Features

1. **RabbitMQ Cluster** (3 nodes)
   - All queues mirrored across all nodes
   - Automatic failover
   - Publisher confirms enabled
   - Persistent messages

2. **Redis Sentinel** (1 master + 2 replicas + 3 sentinels)
   - Automatic master failover
   - Quorum: 2 sentinels
   - AOF persistence on all nodes
   - Synchronous replication

3. **Upload Service** (3 instances)
   - Active-active configuration
   - Shared state via Redis Sentinel
   - Load balancer recommended (HAProxy/Nginx)

4. **Batch Processor** (3 instances)
   - Active-active message consumers
   - Prefetch limit: 1 (fair distribution)
   - Manual acknowledgment

5. **Keycloak** (Single instance with PostgreSQL)
   - Can be clustered if needed (requires additional configuration)
   - Database-backed for persistence

## Prerequisites

### Required Software

1. **Puppet Server** (already in place and functional)
2. **RHEL 8/9** on all 4 servers
3. **Network connectivity** between all servers
4. **DNS or /etc/hosts** configured for server name resolution
5. **Firewall rules** will be managed by Puppet
6. **NFS Server** (optional but recommended for truly shared file storage)

### Server Requirements

**Servers 1-3 (Application Cluster):**
- CPU: 4+ cores
- RAM: 8+ GB
- Disk: 100+ GB
- Network: 1 Gbps

**Server 4 (Keycloak):**
- CPU: 2+ cores
- RAM: 4+ GB
- Disk: 50+ GB
- Network: 1 Gbps

### Network Ports

The following ports will be opened by Puppet:

**All Application Servers (1-3):**
- 5672 (RabbitMQ AMQP)
- 15672 (RabbitMQ Management)
- 25672 (RabbitMQ Clustering)
- 4369 (Erlang Port Mapper)
- 6379 (Redis)
- 26379 (Redis Sentinel)
- 8080 (Upload Service)

**Keycloak Server (4):**
- 8180 (Keycloak HTTP)
- 8543 (Keycloak HTTPS)

## Installation Steps

### 1. Prepare the Puppet Module

Copy this module to your Puppet server's module directory:

```bash
# On Puppet Server
sudo cp -r puppet/manifests /etc/puppetlabs/code/environments/production/modules/fileprocessing/
sudo cp -r puppet/templates /etc/puppetlabs/code/environments/production/modules/fileprocessing/
sudo cp -r puppet/files /etc/puppetlabs/code/environments/production/modules/fileprocessing/
```

### 2. Configure Hiera

Update the Hiera configuration with your environment-specific values:

```bash
# Edit common.yaml
sudo vi /etc/puppetlabs/code/environments/production/data/common.yaml
```

**Update the following critical values:**

```yaml
# Replace with your actual server hostnames or IP addresses
fileprocessing::cluster_servers:
  - 'server1.mydomain.com'  # or IP: 192.168.1.101
  - 'server2.mydomain.com'  # or IP: 192.168.1.102
  - 'server3.mydomain.com'  # or IP: 192.168.1.103

# CHANGE ALL PASSWORDS IN PRODUCTION!
fileprocessing::rabbitmq_password:
  password: 'YOUR_SECURE_RABBITMQ_PASSWORD'

fileprocessing::redis_password:
  password: 'YOUR_SECURE_REDIS_PASSWORD'

fileprocessing::keycloak_admin_password:
  password: 'YOUR_SECURE_KEYCLOAK_ADMIN_PASSWORD'

fileprocessing::postgres_password:
  password: 'YOUR_SECURE_POSTGRES_PASSWORD'

# If using NFS, uncomment and configure:
# fileprocessing::nfs_server: 'nfs.mydomain.com'
# fileprocessing::nfs_export: '/exports/fileprocessing'
```

Copy the Hiera data files:

```bash
sudo cp puppet/hiera/*.yaml /etc/puppetlabs/code/environments/production/data/
```

### 3. Configure Hiera Hierarchy

Edit `/etc/puppetlabs/puppet/hiera.yaml`:

```yaml
---
version: 5
defaults:
  datadir: /etc/puppetlabs/code/environments/production/data
  data_hash: yaml_data

hierarchy:
  - name: "Per-node data"
    path: "%{trusted.certname}.yaml"

  - name: "Common data"
    path: "common.yaml"
```

### 4. Build Application JARs

Before deployment, you need to build the application JARs:

```bash
# On your build machine (where you have the source code)
cd /path/to/WernerTest
mvn clean package -DskipTests

# Copy JARs to Puppet files directory
sudo cp upload-service/target/upload-service-*.jar \
  /etc/puppetlabs/code/environments/production/modules/fileprocessing/files/upload-service.jar

sudo cp batch-processor/target/batch-processor-*.jar \
  /etc/puppetlabs/code/environments/production/modules/fileprocessing/files/batch-processor.jar

# Copy Keycloak realm configuration
sudo cp keycloak/realm-export.json \
  /etc/puppetlabs/code/environments/production/modules/fileprocessing/files/
```

### 5. Configure Site Manifest

Edit `/etc/puppetlabs/code/environments/production/manifests/site.pp`:

```puppet
# Server 1 - Application Cluster Master
node 'server1.mydomain.com' {
  class { 'fileprocessing':
    role            => 'app_cluster',
    server_id       => 1,
    cluster_servers => lookup('fileprocessing::cluster_servers'),
    rabbitmq_password => lookup('fileprocessing::rabbitmq_password'),
    redis_password    => lookup('fileprocessing::redis_password'),
    nfs_server        => lookup('fileprocessing::nfs_server', Optional[String], undef),
    nfs_export        => lookup('fileprocessing::nfs_export', Optional[String], undef),
  }
}

# Server 2 - Application Cluster Node
node 'server2.mydomain.com' {
  class { 'fileprocessing':
    role            => 'app_cluster',
    server_id       => 2,
    cluster_servers => lookup('fileprocessing::cluster_servers'),
    rabbitmq_password => lookup('fileprocessing::rabbitmq_password'),
    redis_password    => lookup('fileprocessing::redis_password'),
    nfs_server        => lookup('fileprocessing::nfs_server', Optional[String], undef),
    nfs_export        => lookup('fileprocessing::nfs_export', Optional[String], undef),
  }
}

# Server 3 - Application Cluster Node
node 'server3.mydomain.com' {
  class { 'fileprocessing':
    role            => 'app_cluster',
    server_id       => 3,
    cluster_servers => lookup('fileprocessing::cluster_servers'),
    rabbitmq_password => lookup('fileprocessing::rabbitmq_password'),
    redis_password    => lookup('fileprocessing::redis_password'),
    nfs_server        => lookup('fileprocessing::nfs_server', Optional[String], undef),
    nfs_export        => lookup('fileprocessing::nfs_export', Optional[String], undef),
  }
}

# Server 4 - Keycloak Authentication Server
node 'server4.mydomain.com' {
  class { 'fileprocessing':
    role                     => 'keycloak',
    server_id                => 4,
    cluster_servers          => [],
    keycloak_admin_password  => lookup('fileprocessing::keycloak_admin_password'),
    postgres_password        => lookup('fileprocessing::postgres_password'),
  }
}
```

### 6. Install Required Puppet Modules

The fileprocessing module uses the `firewalld_port` resource type. Install the firewalld module:

```bash
sudo puppet module install puppet-firewalld
```

### 7. Deploy to Servers

#### Option A: Automated Puppet Agent Run

If Puppet agents are configured on all servers:

```bash
# On each server, or via Puppet orchestration
sudo puppet agent --test
```

#### Option B: Manual Application (Testing)

For testing, you can apply directly:

```bash
# On Server 1
sudo puppet apply --modulepath=/etc/puppetlabs/code/environments/production/modules \
  /etc/puppetlabs/code/environments/production/manifests/site.pp \
  --certname=server1.mydomain.com

# Repeat for servers 2, 3, and 4
```

### 8. Deployment Order

**IMPORTANT:** Deploy in this order for best results:

1. **Server 1 first** (Master node for RabbitMQ and Redis)
2. **Servers 2 and 3** (Join clusters)
3. **Server 4** (Keycloak - can be done in parallel)

Wait for each server to complete before moving to the next.

## Post-Deployment Verification

### 1. Verify RabbitMQ Cluster

```bash
# On any application server
sudo rabbitmqctl cluster_status

# Should show 3 nodes: rabbit@server1, rabbit@server2, rabbit@server3
# All nodes should be running
```

Check RabbitMQ Management UI:
- URL: http://server1.mydomain.com:15672
- Username: fileprocessing
- Password: [your configured password]

### 2. Verify Redis Sentinel

```bash
# On any application server
redis-cli -p 26379 -a [redis_password] SENTINEL masters

# Should show master: mymaster
```

Check Redis replication:

```bash
# On Server 1 (master)
redis-cli -a [redis_password] INFO replication

# Should show role:master and 2 connected slaves
```

### 3. Verify Services

```bash
# Check all services on application servers
sudo systemctl status upload-service
sudo systemctl status batch-processor
sudo systemctl status rabbitmq-server
sudo systemctl status redis
sudo systemctl status redis-sentinel

# All should show "active (running)"
```

### 4. Verify Keycloak

```bash
# On Server 4
docker ps

# Should show keycloak and postgres containers running
```

Access Keycloak:
- URL: http://server4.mydomain.com:8180
- Admin Console: http://server4.mydomain.com:8180/admin
- Username: admin
- Password: [your configured password]

### 5. Test Upload Service

```bash
# From any machine that can reach the servers
curl http://server1.mydomain.com:8080/api/upload/health

# Should return: {"status":"UP"}
```

### 6. Test File Upload (with Authentication)

First, get a JWT token from Keycloak:

```bash
# Get token
TOKEN=$(curl -X POST "http://server4.mydomain.com:8180/realms/file-processing/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=acme.admin" \
  -d "password=password" \
  -d "grant_type=password" \
  -d "client_id=upload-service" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  | jq -r '.access_token')

# Upload a test file
curl -X POST "http://server1.mydomain.com:8080/api/upload" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@testfile.txt"

# Should return upload details with uploadId
```

## High Availability Testing

### Test RabbitMQ Failover

```bash
# Stop RabbitMQ on server 1
sudo systemctl stop rabbitmq-server

# Services should continue using server2 and server3
# Verify by uploading a file - should still work

# Restart server 1
sudo systemctl start rabbitmq-server

# Verify it rejoins the cluster
sudo rabbitmqctl cluster_status
```

### Test Redis Failover

```bash
# Stop Redis on server 1 (current master)
sudo systemctl stop redis

# Wait 5-10 seconds for sentinel to detect and failover
# Check new master
redis-cli -h server2 -p 26379 -a [redis_password] SENTINEL get-master-addr-by-name mymaster

# One of the replicas should now be promoted to master

# Restart server 1
sudo systemctl start redis

# It should rejoin as a replica
```

### Test Upload Service HA

```bash
# Stop upload service on server 1
sudo systemctl stop upload-service

# Upload a file to server2 or server3
curl -X POST "http://server2.mydomain.com:8080/api/upload" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@testfile.txt"

# Should still work

# Restart service
sudo systemctl start upload-service
```

## Load Balancing (Recommended)

For production, add a load balancer (HAProxy or Nginx) in front of the upload services:

### HAProxy Example Configuration

```haproxy
frontend upload_frontend
    bind *:80
    default_backend upload_backend

backend upload_backend
    balance roundrobin
    option httpchk GET /api/upload/health
    server server1 server1.mydomain.com:8080 check
    server server2 server2.mydomain.com:8080 check
    server server3 server3.mydomain.com:8080 check
```

### Nginx Example Configuration

```nginx
upstream upload_service {
    server server1.mydomain.com:8080;
    server server2.mydomain.com:8080;
    server server3.mydomain.com:8080;
}

server {
    listen 80;

    location / {
        proxy_pass http://upload_service;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## Monitoring

### Key Metrics to Monitor

1. **RabbitMQ:**
   - Queue depths
   - Message rates
   - Cluster health
   - Memory usage

2. **Redis:**
   - Master/replica status
   - Sentinel health
   - Memory usage
   - Hit rate

3. **Services:**
   - CPU and memory usage
   - Response times
   - Error rates
   - Upload success rates

### Log Locations

```
/var/log/fileprocessing/upload-service.log
/var/log/fileprocessing/batch-processor.log
/var/log/redis/redis.log
/var/log/redis/sentinel.log
/var/log/rabbitmq/
```

## Troubleshooting

### RabbitMQ Issues

**Problem:** Nodes not joining cluster

```bash
# Check Erlang cookie matches on all nodes
sudo cat /var/lib/rabbitmq/.erlang.cookie

# Should be identical on all servers

# Check network connectivity
telnet server1 5672
telnet server1 25672
telnet server1 4369

# Manually reset and rejoin node
sudo rabbitmqctl stop_app
sudo rabbitmqctl reset
sudo rabbitmqctl join_cluster rabbit@server1
sudo rabbitmqctl start_app
```

**Problem:** Permission denied errors

```bash
# Verify vhost and user
sudo rabbitmqctl list_vhosts
sudo rabbitmqctl list_users
sudo rabbitmqctl list_permissions -p /fileprocessing
```

### Redis Issues

**Problem:** Sentinel not detecting master

```bash
# Check sentinel configuration
redis-cli -p 26379 -a [password] SENTINEL master mymaster

# Force failover (testing only)
redis-cli -p 26379 -a [password] SENTINEL failover mymaster

# Check sentinel logs
sudo journalctl -u redis-sentinel -f
```

**Problem:** Replication lag

```bash
# Check replication offset
redis-cli -a [password] INFO replication
```

### Service Issues

**Problem:** Upload service not starting

```bash
# Check logs
sudo journalctl -u upload-service -f

# Verify Java installation
java -version

# Check if ports are in use
sudo netstat -tlnp | grep 8080

# Verify application.properties
cat /opt/fileprocessing/upload-service/application.properties
```

**Problem:** Cannot connect to RabbitMQ/Redis

```bash
# Test connectivity
telnet server1 5672  # RabbitMQ
telnet server1 6379  # Redis

# Check firewall
sudo firewall-cmd --list-all

# Temporarily disable firewall for testing
sudo systemctl stop firewalld
```

### Keycloak Issues

**Problem:** Keycloak not accessible

```bash
# Check Docker containers
docker ps
docker logs keycloak
docker logs keycloak-postgres

# Restart Keycloak
cd /opt/fileprocessing/keycloak
docker-compose restart

# Check PostgreSQL connection
docker exec -it keycloak-postgres psql -U keycloak -d keycloak -c "\dt"
```

## Backup and Recovery

### Backup Procedures

**RabbitMQ:**
```bash
# Export definitions (users, vhosts, policies)
sudo rabbitmqctl export_definitions /backup/rabbitmq-definitions.json
```

**Redis:**
```bash
# RDB snapshots are automatic
# AOF files at: /var/lib/redis/appendonly.aof
# Copy for backup:
sudo cp /var/lib/redis/dump.rdb /backup/
sudo cp /var/lib/redis/appendonly.aof /backup/
```

**Keycloak:**
```bash
# Backup PostgreSQL database
docker exec keycloak-postgres pg_dump -U keycloak keycloak > /backup/keycloak-db.sql

# Backup realm configuration
docker exec keycloak /opt/keycloak/bin/kc.sh export --file /tmp/realm-backup.json
docker cp keycloak:/tmp/realm-backup.json /backup/
```

**Uploaded Files:**
- If using NFS: Backup NFS server
- If local: Backup `/var/lib/fileprocessing/uploads` on all servers

## Security Hardening

### Production Checklist

- [ ] Change all default passwords in Hiera
- [ ] Enable SELinux (currently set to permissive)
- [ ] Configure TLS for RabbitMQ
- [ ] Configure TLS for Redis
- [ ] Configure HTTPS for Keycloak
- [ ] Configure HTTPS for Upload Service
- [ ] Restrict firewall rules to specific source IPs
- [ ] Enable audit logging
- [ ] Set up monitoring and alerting
- [ ] Configure log rotation
- [ ] Enable automated backups
- [ ] Implement intrusion detection
- [ ] Review and harden SSH configuration
- [ ] Enable two-factor authentication in Keycloak
- [ ] Regular security patching schedule

## Maintenance

### Update Application

```bash
# Build new JAR
mvn clean package -DskipTests

# Copy to Puppet files
sudo cp target/upload-service-*.jar /etc/puppetlabs/code/environments/production/modules/fileprocessing/files/upload-service.jar

# Run Puppet on all servers
sudo puppet agent --test

# Or restart services manually on each server
sudo systemctl restart upload-service
sudo systemctl restart batch-processor
```

### Scale Horizontally

To add more application servers:

1. Provision new RHEL server
2. Add to Hiera cluster_servers list
3. Create new node definition in site.pp
4. Run Puppet agent
5. Services will automatically join clusters

## Support and Documentation

- **Main README:** /path/to/WernerTest/README.md
- **Authentication Guide:** /path/to/WernerTest/AUTHENTICATION.md
- **Puppet Module:** /etc/puppetlabs/code/environments/production/modules/fileprocessing/
- **Logs:** /var/log/fileprocessing/

For issues or questions, refer to the project documentation or contact your system administrator.

---

**Version:** 1.0
**Last Updated:** 2025-11-18
**Maintained by:** File Processing System Team
