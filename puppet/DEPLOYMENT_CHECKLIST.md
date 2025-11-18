# Puppet Deployment Checklist

## Pre-Deployment

### Infrastructure
- [ ] 4 RHEL 8/9 servers provisioned in VMware cluster
- [ ] Servers have network connectivity to each other
- [ ] DNS configured or /etc/hosts updated with all server names
- [ ] NFS server configured (if using shared storage)
- [ ] Puppet Server installed and functional
- [ ] Puppet agents installed on all 4 servers (or Puppet apply will be used)

### Server Specifications Met
- [ ] Servers 1-3: 4+ CPU cores, 8+ GB RAM, 100+ GB disk
- [ ] Server 4: 2+ CPU cores, 4+ GB RAM, 50+ GB disk
- [ ] Network: 1 Gbps minimum

### Access and Credentials
- [ ] Root or sudo access to all servers
- [ ] SSH keys configured for Puppet access
- [ ] Firewall access between servers confirmed

## Puppet Configuration

### Module Installation
- [ ] Copied manifests to `/etc/puppetlabs/code/environments/production/modules/fileprocessing/manifests/`
- [ ] Copied templates to `/etc/puppetlabs/code/environments/production/modules/fileprocessing/templates/`
- [ ] Created files directory at `/etc/puppetlabs/code/environments/production/modules/fileprocessing/files/`
- [ ] Installed puppet-firewalld module: `puppet module install puppet-firewalld`

### Application Build
- [ ] Built upload-service JAR: `mvn clean package -DskipTests`
- [ ] Copied upload-service JAR to Puppet files directory
- [ ] Built batch-processor JAR
- [ ] Copied batch-processor JAR to Puppet files directory
- [ ] Copied realm-export.json to Puppet files directory

### Hiera Configuration
- [ ] Copied Hiera YAML files to `/etc/puppetlabs/code/environments/production/data/`
- [ ] Updated common.yaml with actual server hostnames/IPs
- [ ] **CHANGED ALL PASSWORDS in common.yaml**
  - [ ] RabbitMQ password
  - [ ] Redis password
  - [ ] Keycloak admin password
  - [ ] PostgreSQL password
- [ ] Configured NFS settings (if applicable)
- [ ] Updated hiera.yaml at `/etc/puppetlabs/puppet/hiera.yaml`

### Site Manifest
- [ ] Copied site.pp.example to `/etc/puppetlabs/code/environments/production/manifests/site.pp`
- [ ] Updated node names in site.pp with actual server names
- [ ] Verified Hiera lookups are correct

## Deployment

### Deployment Order
- [ ] **Step 1:** Deploy to Server 1 (Master node)
  ```bash
  sudo puppet agent --test
  ```
  - [ ] Wait for completion
  - [ ] Verify RabbitMQ is running
  - [ ] Verify Redis master is running
  - [ ] Verify Redis Sentinel is running

- [ ] **Step 2:** Deploy to Server 2
  ```bash
  sudo puppet agent --test
  ```
  - [ ] Wait for completion
  - [ ] Verify joined RabbitMQ cluster
  - [ ] Verify Redis replica is running

- [ ] **Step 3:** Deploy to Server 3
  ```bash
  sudo puppet agent --test
  ```
  - [ ] Wait for completion
  - [ ] Verify joined RabbitMQ cluster
  - [ ] Verify Redis replica is running

- [ ] **Step 4:** Deploy to Server 4 (Keycloak)
  ```bash
  sudo puppet agent --test
  ```
  - [ ] Wait for completion
  - [ ] Verify Keycloak container is running
  - [ ] Verify PostgreSQL container is running

### Return to Application Servers
- [ ] Upload service started on all three servers
- [ ] Batch processor started on all three servers

## Post-Deployment Verification

### RabbitMQ Cluster
- [ ] Cluster status shows 3 running nodes
  ```bash
  sudo rabbitmqctl cluster_status
  ```
- [ ] Management UI accessible on port 15672
- [ ] Can login with configured credentials
- [ ] HA policy applied to queues
- [ ] Virtual host `/fileprocessing` exists
- [ ] User `fileprocessing` has correct permissions

### Redis Sentinel
- [ ] Sentinel running on all 3 servers
  ```bash
  redis-cli -p 26379 -a PASSWORD SENTINEL masters
  ```
- [ ] Master detected on Server 1
- [ ] Two replicas connected
- [ ] All sentinels monitoring the master

### Services
- [ ] Upload service running on servers 1, 2, 3
  ```bash
  sudo systemctl status upload-service
  ```
- [ ] Batch processor running on servers 1, 2, 3
  ```bash
  sudo systemctl status batch-processor
  ```
- [ ] Health endpoints respond:
  ```bash
  curl http://server1:8080/api/upload/health
  curl http://server2:8080/api/upload/health
  curl http://server3:8080/api/upload/health
  ```

### Keycloak
- [ ] Keycloak accessible at http://server4:8180
- [ ] Admin console accessible
- [ ] Can login with admin credentials
- [ ] Realm `file-processing` imported
- [ ] Test users exist (acme.admin, acme.user1, etc.)
- [ ] Clients configured (upload-service, upload-web-app)

### Network and Firewall
- [ ] Firewall rules applied correctly
  ```bash
  sudo firewall-cmd --list-all
  ```
- [ ] Can telnet to RabbitMQ port 5672 from all servers
- [ ] Can telnet to Redis port 6379 from all servers
- [ ] Can access upload service port 8080 from client machines

### File Storage
- [ ] NFS mount successful (if configured)
  ```bash
  df -h | grep fileprocessing
  ```
- [ ] Upload directory writable
  ```bash
  sudo -u fileprocessing touch /var/lib/fileprocessing/uploads/test.txt
  ```

## Functional Testing

### Authentication Test
- [ ] Obtain JWT token from Keycloak
  ```bash
  TOKEN=$(curl -X POST "http://server4:8180/realms/file-processing/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=acme.admin" \
    -d "password=password" \
    -d "grant_type=password" \
    -d "client_id=upload-web-app" \
    | jq -r '.access_token')
  echo $TOKEN
  ```
- [ ] Token received successfully

### File Upload Test
- [ ] Upload test file
  ```bash
  curl -X POST "http://server1:8080/api/upload" \
    -H "Authorization: Bearer $TOKEN" \
    -F "file=@testfile.txt"
  ```
- [ ] Upload successful, received uploadId
- [ ] File exists in upload directory
- [ ] Message sent to RabbitMQ (check management UI)
- [ ] Batch processor receives message (check logs)

### Web UI Test
- [ ] Access web UI: http://server1:8080/
- [ ] Redirects to login page
- [ ] Can login with test credentials
- [ ] Dashboard loads successfully
- [ ] Can upload file via web interface
- [ ] Upload progress monitored in real-time
- [ ] Admin dashboard accessible for admin users

## High Availability Testing

### RabbitMQ Failover
- [ ] Stop RabbitMQ on server 1
  ```bash
  sudo systemctl stop rabbitmq-server
  ```
- [ ] Upload still works via server 2 or 3
- [ ] Restart RabbitMQ on server 1
- [ ] Node rejoins cluster successfully

### Redis Failover
- [ ] Stop Redis on server 1 (current master)
  ```bash
  sudo systemctl stop redis
  ```
- [ ] Sentinel promotes new master (check with SENTINEL masters)
- [ ] Upload service continues working (using new master)
- [ ] Restart Redis on server 1
- [ ] Server 1 rejoins as replica

### Service Failover
- [ ] Stop upload-service on server 1
- [ ] Upload works via server 2 or 3
- [ ] Stop upload-service on server 2
- [ ] Upload works via server 3
- [ ] Restart services on servers 1 and 2

## Load Balancer Configuration (Optional)

If using load balancer:
- [ ] HAProxy or Nginx configured
- [ ] Health checks configured for upload service
- [ ] All three servers in backend pool
- [ ] Failover tested
- [ ] Sticky sessions configured (if needed)

## Monitoring Setup (Recommended)

- [ ] Prometheus/Grafana installed
- [ ] Metrics scraped from:
  - [ ] RabbitMQ
  - [ ] Redis
  - [ ] Upload Service actuator endpoints
- [ ] Alerts configured for:
  - [ ] Service down
  - [ ] Queue depth high
  - [ ] Memory usage high
  - [ ] Disk space low

## Backup Configuration

- [ ] RabbitMQ definitions exported
- [ ] Redis backup script configured
- [ ] Keycloak database backup configured
- [ ] Uploaded files backup configured
- [ ] Backup restore tested

## Security Hardening

### Immediate (Critical)
- [ ] All default passwords changed
- [ ] Firewall rules restricted to necessary ports
- [ ] SSH key-based authentication only
- [ ] Root login disabled

### Phase 2 (Recommended)
- [ ] SELinux re-enabled with proper policies
- [ ] TLS configured for all services
- [ ] Intrusion detection system deployed
- [ ] Log aggregation configured
- [ ] Regular security scanning scheduled

## Documentation

- [ ] Network diagram created
- [ ] Passwords stored in secure vault (not in plain text)
- [ ] Runbook created for common operations
- [ ] Disaster recovery procedure documented
- [ ] On-call contact list updated

## Sign-off

- [ ] System tested by development team
- [ ] System tested by QA team
- [ ] Performance benchmarks met
- [ ] Security review completed
- [ ] Operations team trained
- [ ] Documentation handoff completed

---

**Deployment Date:** _________________

**Deployed By:** _________________

**Reviewed By:** _________________

**Production Ready:** [ ] YES  [ ] NO

**Notes:**

