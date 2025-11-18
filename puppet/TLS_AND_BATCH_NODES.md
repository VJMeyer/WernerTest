# TLS Configuration and Standalone Batch Processor Nodes

This guide explains how to deploy batch processor services on separate nodes (closer to databases or wiskBat installations) and secure RabbitMQ connections with TLS over port 443.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [TLS Certificate Generation](#tls-certificate-generation)
3. [Deploying RabbitMQ with TLS](#deploying-rabbitmq-with-tls)
4. [Deploying Standalone Batch Nodes](#deploying-standalone-batch-nodes)
5. [Network Architecture](#network-architecture)
6. [Testing and Verification](#testing-and-verification)
7. [Troubleshooting](#troubleshooting)

## Architecture Overview

### Standard Architecture (Without TLS)

```
┌────────────────┐     ┌────────────────┐     ┌────────────────┐
│   Server 1     │     │   Server 2     │     │   Server 3     │
├────────────────┤     ├────────────────┤     ├────────────────┤
│  RabbitMQ      │◄───►│  RabbitMQ      │◄───►│  RabbitMQ      │
│  (port 5672)   │     │  (port 5672)   │     │  (port 5672)   │
│                │     │                │     │                │
│  Upload Svc    │     │  Upload Svc    │     │  Upload Svc    │
│  Batch Proc    │     │  Batch Proc    │     │  Batch Proc    │
└────────────────┘     └────────────────┘     └────────────────┘
```

### Enhanced Architecture (With TLS and Remote Batch Nodes)

```
┌────────────────┐     ┌────────────────┐     ┌────────────────┐
│   Server 1     │     │   Server 2     │     │   Server 3     │
├────────────────┤     ├────────────────┤     ├────────────────┤
│  RabbitMQ      │◄───►│  RabbitMQ      │◄───►│  RabbitMQ      │
│  (port 5672)   │     │  (port 5672)   │     │  (port 5672)   │
│                │     │                │     │                │
│  HAProxy       │     │  HAProxy       │     │  HAProxy       │
│  (TLS port 443)│     │  (TLS port 443)│     │  (TLS port 443)│
│                │     │                │     │                │
│  Upload Svc    │     │  Upload Svc    │     │  Upload Svc    │
└────────────────┘     └────────────────┘     └────────────────┘
        ▲                      ▲                      ▲
        │                      │                      │
        │ TLS/443              │ TLS/443              │ TLS/443
        │                      │                      │
┌───────┴──────────────────────┴──────────────────────┴───────┐
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Batch Node 1│  │  Batch Node 2│  │  Batch Node N│      │
│  ├──────────────┤  ├──────────────┤  ├──────────────┤      │
│  │ Batch Proc   │  │ Batch Proc   │  │ Batch Proc   │      │
│  │ wiskBat      │  │ wiskBat      │  │ wiskBat      │      │
│  │ Database     │  │ Database     │  │ Database     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                              │
│  (Near databases or Windows systems with wiskBat)           │
└──────────────────────────────────────────────────────────────┘
```

## Benefits

### 1. Deploying Batch Processors Separately

**Why?**
- **Proximity to Data**: Place batch processors on servers near databases or file storage
- **Windows Integration**: Deploy on Windows servers that have wiskBat installed
- **Resource Isolation**: Separate batch processing from web/upload services
- **Scalability**: Add more batch processors independently based on workload
- **Network Segmentation**: Keep batch processing in internal networks

**Use Cases:**
- Batch processors on database servers (reduce latency)
- Batch processors on Windows servers with wiskBat
- Batch processors in different data centers
- Scaling batch processing independently of upload capacity

### 2. TLS Encryption over Port 443

**Why?**
- **Security**: Encrypted communication between batch nodes and RabbitMQ
- **Firewall Friendly**: Port 443 (HTTPS) typically allowed through firewalls
- **Network Segmentation**: Connect batch processors across network boundaries
- **Compliance**: Meet security requirements for encrypted messaging
- **Standard Port**: No special firewall rules needed for outbound connections

## TLS Certificate Generation

### Prerequisites

- OpenSSL installed on your workstation
- Java keytool (part of JDK) for creating trust stores
- Access to server hostnames/IPs

### Step 1: Generate Certificates

We've provided a script to generate all necessary certificates:

```bash
cd /path/to/WernerTest/puppet/files
./generate-rabbitmq-certs.sh server1.example.com server2.example.com server3.example.com
```

This script creates:
- **CA Certificate and Key** - Signs all server certificates
- **Server Certificates** - One for each RabbitMQ server
- **HAProxy Certificate** - Combined cert+key for HAProxy
- **Java TrustStore** - For batch processors to verify server certificates

### Step 2: Copy Certificates to Puppet Module

```bash
# Navigate to certificate directory
cd rabbitmq-certs

# Copy to Puppet files directory
sudo cp ca/ca-cert.pem /etc/puppetlabs/code/environments/production/modules/fileprocessing/files/rabbitmq-ca-cert.pem

sudo cp server/server1.example.com-cert.pem /etc/puppetlabs/code/environments/production/modules/fileprocessing/files/rabbitmq-server-cert.pem

sudo cp server/server1.example.com-key.pem /etc/puppetlabs/code/environments/production/modules/fileprocessing/files/rabbitmq-server-key.pem

sudo cp server/haproxy-rabbitmq.pem /etc/puppetlabs/code/environments/production/modules/fileprocessing/files/

sudo cp server/rabbitmq-truststore.jks /etc/puppetlabs/code/environments/production/modules/fileprocessing/files/
```

**Note**: If your servers have different hostnames, you'll need to copy the appropriate server certificate for each. Consider using Hiera per-node data for this.

### Step 3: Secure the CA Private Key

```bash
# Store CA key securely - you'll need it to sign new certificates
sudo cp ca/ca-key.pem /secure/location/rabbitmq-ca-key.pem
sudo chmod 600 /secure/location/rabbitmq-ca-key.pem

# Remove from working directory
rm -rf rabbitmq-certs
```

## Deploying RabbitMQ with TLS

### Step 1: Enable TLS in Hiera

Edit `/etc/puppetlabs/code/environments/production/data/common.yaml`:

```yaml
# Enable TLS for RabbitMQ connections
fileprocessing::enable_rabbitmq_tls: true
fileprocessing::use_tls: true
```

### Step 2: Update Site Manifest

Edit `/etc/puppetlabs/code/environments/production/manifests/site.pp`:

```puppet
# Server 1 - Application Cluster Master with TLS
node 'server1.example.com' {
  class { 'fileprocessing':
    role                 => 'app_cluster',
    server_id            => 1,
    cluster_servers      => lookup('fileprocessing::cluster_servers'),
    enable_rabbitmq_tls  => true,  # Enable TLS support
    use_tls              => true,  # Applications use TLS
    rabbitmq_password    => lookup('fileprocessing::rabbitmq_password'),
    redis_password       => lookup('fileprocessing::redis_password'),
  }
}

# Repeat for servers 2 and 3 with enable_rabbitmq_tls => true
```

### Step 3: Deploy to Application Servers

```bash
# Deploy to each application server in order
# Server 1 first
sudo puppet agent --test

# Then servers 2 and 3
sudo puppet agent --test
```

### What Gets Installed

On each application server (1-3):
- **RabbitMQ with TLS** listener on port 5671 (AMQPS)
- **HAProxy** for TLS termination on port 443
  - Accepts TLS connections on port 443
  - Forwards to local RabbitMQ on port 5672
  - Health checks configured
- **Firewall rules** for ports 443 and 5671
- **TLS certificates** deployed to `/etc/rabbitmq/tls/`

### Verify TLS Configuration

```bash
# Check HAProxy is running
sudo systemctl status haproxy

# Check HAProxy is listening on port 443
sudo netstat -tlnp | grep 443

# Test TLS connection
openssl s_client -connect server1.example.com:443 -showcerts

# Check RabbitMQ TLS listener
sudo rabbitmqctl status | grep -A5 listeners

# View HAProxy stats
curl http://server1.example.com:8404/stats
```

## Deploying Standalone Batch Nodes

Standalone batch processor nodes can be deployed on any server - Linux, Windows (with WSL), or near databases.

### Step 1: Create Hiera Data for Batch Node

Create `/etc/puppetlabs/code/environments/production/data/batch1.yaml`:

```yaml
---
# Hiera data for Batch Node 1

fileprocessing::role: 'batch_node'
fileprocessing::server_id: 5
fileprocessing::use_tls: true

# Cluster servers to connect to (for RabbitMQ)
fileprocessing::cluster_servers:
  - 'server1.example.com'
  - 'server2.example.com'
  - 'server3.example.com'

# RabbitMQ password (same as cluster)
fileprocessing::rabbitmq_password:
  password: 'your_rabbitmq_password'
```

### Step 2: Add Node to Site Manifest

Edit `/etc/puppetlabs/code/environments/production/manifests/site.pp`:

```puppet
# Batch Node 1 - Near database or wiskBat installation
node 'batch1.example.com' {
  class { 'fileprocessing':
    role            => 'batch_node',
    server_id       => 5,
    cluster_servers => lookup('fileprocessing::cluster_servers'),
    use_tls         => true,  # Connect via TLS on port 443
    rabbitmq_password => lookup('fileprocessing::rabbitmq_password'),
  }
}

# Additional batch nodes
node 'batch2.example.com' {
  class { 'fileprocessing':
    role            => 'batch_node',
    server_id       => 6,
    cluster_servers => lookup('fileprocessing::cluster_servers'),
    use_tls         => true,
    rabbitmq_password => lookup('fileprocessing::rabbitmq_password'),
  }
}
```

### Step 3: Deploy Batch Node

```bash
# On the batch node server
sudo puppet agent --test
```

### What Gets Installed

On each batch node:
- **Java 21** (OpenJDK)
- **Batch Processor** service as systemd unit
- **TLS Trust Store** (`/opt/fileprocessing/batch-processor/truststore.jks`)
- **Application properties** configured for TLS connections to RabbitMQ cluster
- **Firewall** configured for outbound connections to RabbitMQ (port 443)
- **Log files** at `/var/log/fileprocessing/batch-processor.log`

### Verify Batch Node

```bash
# Check service is running
sudo systemctl status batch-processor

# Check configuration
cat /opt/fileprocessing/batch-processor/application.properties

# Verify TLS trust store exists
ls -l /opt/fileprocessing/batch-processor/truststore.jks

# Check logs for successful connection
sudo journalctl -u batch-processor -f

# Should see lines like:
# Successfully connected to RabbitMQ over TLS
# Listening for messages on queue: file.upload.queue
```

### Configure wiskBat Path

Edit the application properties on the batch node:

```bash
sudo vi /opt/fileprocessing/batch-processor/application.properties

# Update this line:
batch.wiski-bat-path=/actual/path/to/wiskBat

# Restart service
sudo systemctl restart batch-processor
```

## Network Architecture

### Port Usage Summary

**Application Cluster Servers (1-3):**
- Port 5672: RabbitMQ AMQP (internal cluster communication)
- Port 5671: RabbitMQ AMQPS (TLS native, optional)
- Port 443: HAProxy TLS termination → RabbitMQ
- Port 15672: RabbitMQ Management UI
- Port 25672: RabbitMQ clustering
- Port 4369: Erlang Port Mapper
- Port 6379: Redis
- Port 26379: Redis Sentinel
- Port 8080: Upload Service
- Port 8404: HAProxy statistics (optional)

**Batch Nodes:**
- Outbound only to port 443 on application servers
- No inbound ports required

**Keycloak Server (4):**
- Port 8180: Keycloak HTTP
- Port 8543: Keycloak HTTPS

### Firewall Configuration

**On Application Servers:**
```bash
# Automatically configured by Puppet
sudo firewall-cmd --list-all

# Should show:
# - 443/tcp (HAProxy TLS)
# - 5671/tcp (RabbitMQ AMQPS)
# - 5672/tcp (RabbitMQ AMQP)
# - etc.
```

**On Batch Nodes:**
```bash
# Only outbound connections needed
# No inbound firewall rules required

# Test connectivity to RabbitMQ cluster
telnet server1.example.com 443
telnet server2.example.com 443
telnet server3.example.com 443
```

**Network Diagram:**

```
Internet/DMZ
     │
     │ Firewall
     ▼
┌─────────────────────────────────────┐
│  Application Cluster Network        │
│  (Upload services, RabbitMQ, Redis) │
│                                     │
│  Server 1, 2, 3                     │
│  Ports: 443, 8080 (public)          │
│                                     │
└──────────────┬──────────────────────┘
               │
               │ Port 443/TLS
               │ (Secured)
               ▼
┌─────────────────────────────────────┐
│  Internal Network                   │
│  (Database, Batch Processing)       │
│                                     │
│  Batch Nodes 1, 2, N                │
│  (No inbound ports)                 │
│                                     │
└─────────────────────────────────────┘
```

## Testing and Verification

### Test TLS Connection from Batch Node

```bash
# On batch node, test TLS connection to RabbitMQ
openssl s_client -connect server1.example.com:443 -CAfile /opt/fileprocessing/batch-processor/ca-cert.pem

# Should show:
# - TLS handshake details
# - Server certificate
# - Verification: OK
```

### Test Message Flow

**1. Upload a file to trigger message:**

```bash
# Get JWT token from Keycloak
TOKEN=$(curl -X POST "http://server4.example.com:8180/realms/file-processing/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=acme.admin" \
  -d "password=password" \
  -d "grant_type=password" \
  -d "client_id=upload-web-app" \
  | jq -r '.access_token')

# Upload test file
curl -X POST "http://server1.example.com:8080/api/upload" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@testfile.csv"
```

**2. Verify message in RabbitMQ:**

```bash
# Check queue depth
sudo rabbitmqctl list_queues -p /fileprocessing name messages

# Should show file.upload.queue with 1 message
```

**3. Check batch processor receives it:**

```bash
# On batch node, check logs
sudo journalctl -u batch-processor -f

# Should see:
# Received file upload message: {uploadId=..., filename=testfile.csv}
# Processing file: testfile.csv
# Generated batch file: /opt/fileprocessing/batch-output/testfile.bat
# Calling wiskBat with batch file
```

**4. Verify batch file created:**

```bash
# On batch node
ls -la /opt/fileprocessing/batch-output/

# Should see .bat file for the uploaded file
```

### Load Testing TLS Connections

```bash
# Install load testing tool
sudo yum install -y httpd-tools

# Test HAProxy TLS termination (adjust concurrency as needed)
ab -n 1000 -c 10 -k https://server1.example.com:443/
```

### Monitor HAProxy

```bash
# Access HAProxy statistics page
firefox http://server1.example.com:8404/stats

# Or via curl
curl http://server1.example.com:8404/stats

# Shows:
# - Frontend connections
# - Backend server health
# - Request rates
# - Error rates
```

## Troubleshooting

### Issue: Batch Node Cannot Connect to RabbitMQ

**Symptoms:**
```
Connection refused to server1.example.com:443
```

**Diagnosis:**
```bash
# On batch node, test connectivity
telnet server1.example.com 443

# Test TLS handshake
openssl s_client -connect server1.example.com:443
```

**Solutions:**
1. Check firewall on application servers:
   ```bash
   sudo firewall-cmd --list-all | grep 443
   ```

2. Verify HAProxy is running:
   ```bash
   sudo systemctl status haproxy
   ```

3. Check HAProxy logs:
   ```bash
   sudo journalctl -u haproxy -f
   ```

### Issue: TLS Certificate Verification Failed

**Symptoms:**
```
PKIX path building failed: unable to find valid certification path
```

**Diagnosis:**
```bash
# Verify trust store exists
ls -l /opt/fileprocessing/batch-processor/truststore.jks

# Check trust store contents
keytool -list -keystore /opt/fileprocessing/batch-processor/truststore.jks -storepass changeit
```

**Solutions:**
1. Ensure CA certificate is in trust store:
   ```bash
   keytool -list -alias rabbitmq-ca -keystore /opt/fileprocessing/batch-processor/truststore.jks -storepass changeit
   ```

2. Re-import CA certificate:
   ```bash
   sudo keytool -delete -alias rabbitmq-ca -keystore /opt/fileprocessing/batch-processor/truststore.jks -storepass changeit
   sudo keytool -import -trustcacerts -alias rabbitmq-ca -file /path/to/ca-cert.pem \
     -keystore /opt/fileprocessing/batch-processor/truststore.jks -storepass changeit -noprompt
   ```

3. Restart batch processor:
   ```bash
   sudo systemctl restart batch-processor
   ```

### Issue: HAProxy Backend Servers Down

**Symptoms:**
```
HAProxy stats page shows all backend servers as DOWN
```

**Diagnosis:**
```bash
# Check RabbitMQ is listening on 5672
sudo netstat -tlnp | grep 5672

# Test local connection
telnet localhost 5672
```

**Solutions:**
1. Restart RabbitMQ:
   ```bash
   sudo systemctl restart rabbitmq-server
   ```

2. Check RabbitMQ logs:
   ```bash
   sudo journalctl -u rabbitmq-server -f
   ```

3. Verify RabbitMQ cluster health:
   ```bash
   sudo rabbitmqctl cluster_status
   ```

### Issue: Batch Processor Connecting to Wrong Port

**Symptoms:**
```
Batch processor tries to connect to port 5672 instead of 443
```

**Solution:**

Check application.properties:
```bash
cat /opt/fileprocessing/batch-processor/application.properties | grep port

# Should show:
# spring.rabbitmq.port=443 (if TLS enabled)
# spring.rabbitmq.port=5672 (if TLS disabled)
```

If incorrect, re-run Puppet:
```bash
sudo puppet agent --test
```

### Issue: Certificate Hostname Mismatch

**Symptoms:**
```
Certificate doesn't match requested hostname
```

**Cause:**
Server hostname doesn't match certificate CN or SAN entries.

**Solution:**

Regenerate certificate with correct hostnames:
```bash
./generate-rabbitmq-certs.sh actual-server1.com actual-server2.com actual-server3.com
```

Copy new certificates to Puppet and redeploy.

## Security Best Practices

### Certificate Management

1. **Protect CA Private Key**
   - Store in secure location
   - Encrypt with passphrase
   - Limit access (chmod 600)

2. **Certificate Rotation**
   - Certificates valid for 10 years by default
   - Plan rotation before expiry
   - Update all servers simultaneously

3. **Trust Store Password**
   - Change default "changeit" password
   - Store in Hiera encrypted data
   - Use eyaml or Vault for secrets

### Network Security

1. **Restrict Port 443**
   - Limit source IPs for batch nodes if possible
   - Use VPN or private networks

2. **Monitor Connections**
   - Review HAProxy logs regularly
   - Alert on failed authentication attempts
   - Monitor unusual traffic patterns

3. **Regular Updates**
   - Keep OpenSSL updated
   - Update RabbitMQ and HAProxy
   - Apply security patches promptly

## Advanced Configuration

### Multiple Data Centers

If batch nodes are in different data centers:

```yaml
# Hiera for batch node in datacenter 2
fileprocessing::cluster_servers:
  - 'server1.dc1.example.com'
  - 'server2.dc1.example.com'
  - 'server3.dc1.example.com'

# Use nearest server first for better performance
```

### Windows Batch Nodes

For deploying on Windows servers with WSL:

1. Install WSL2 on Windows Server
2. Install Puppet agent in WSL
3. Deploy as batch_node role
4. Configure wiskBat path to Windows program:
   ```
   batch.wiski-bat-path=/mnt/c/Program Files/WiskBat/wiskBat.exe
   ```

### Custom Cipher Suites

To customize TLS cipher suites, edit:
```
puppet/templates/haproxy-rabbitmq.cfg.erb
puppet/templates/rabbitmq-tls.conf.erb
```

Add only strong ciphers for your organization's security policy.

## Migration Path

### Migrating from Non-TLS to TLS

1. **Phase 1: Add TLS Support (No Disruption)**
   - Deploy HAProxy with TLS on port 443
   - Keep existing port 5672 connections
   - Both TLS and non-TLS work simultaneously

2. **Phase 2: Update Batch Nodes (Gradual)**
   - Update batch nodes one at a time to use TLS
   - Test each before moving to next
   - Can roll back by changing use_tls to false

3. **Phase 3: Disable Non-TLS (Optional)**
   - After all batch nodes use TLS
   - Can disable port 5672 if desired (for external connections)
   - Keep port 5672 for internal cluster communication

### Rolling Back

If issues occur:

```yaml
# In Hiera common.yaml
fileprocessing::enable_rabbitmq_tls: false
fileprocessing::use_tls: false
```

Run Puppet to revert:
```bash
sudo puppet agent --test
```

---

**Next Steps:**
1. Generate certificates: `puppet/files/generate-rabbitmq-certs.sh`
2. Enable TLS in Hiera: `fileprocessing::enable_rabbitmq_tls: true`
3. Deploy to application servers
4. Add batch node definitions to site.pp
5. Deploy batch nodes
6. Test and verify!

For questions or issues, refer to the main [puppet/README.md](README.md) or the troubleshooting section above.
