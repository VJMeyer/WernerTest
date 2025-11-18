#!/bin/bash
#
# Generate TLS certificates for RabbitMQ
# This script creates a CA, server certificates, and Java trust stores
#
# Usage: ./generate-rabbitmq-certs.sh <server1_hostname> <server2_hostname> <server3_hostname>
#
# Example: ./generate-rabbitmq-certs.sh server1.example.com server2.example.com server3.example.com
#

set -e

if [ "$#" -lt 3 ]; then
    echo "Usage: $0 <server1_hostname> <server2_hostname> <server3_hostname>"
    echo "Example: $0 server1.example.com server2.example.com server3.example.com"
    exit 1
fi

SERVER1=$1
SERVER2=$2
SERVER3=$3

CERT_DIR="./rabbitmq-certs"
CA_DIR="$CERT_DIR/ca"
SERVER_DIR="$CERT_DIR/server"

mkdir -p $CA_DIR $SERVER_DIR

echo "==> Generating CA certificate..."

# Generate CA private key
openssl genrsa -out $CA_DIR/ca-key.pem 4096

# Generate CA certificate
openssl req -new -x509 -days 3650 -key $CA_DIR/ca-key.pem -out $CA_DIR/ca-cert.pem \
    -subj "/C=US/ST=State/L=City/O=Organization/OU=IT/CN=RabbitMQ CA"

echo "==> Generating server certificates for $SERVER1, $SERVER2, $SERVER3..."

# Create server certificate for each node
for SERVER in $SERVER1 $SERVER2 $SERVER3; do
    echo "  -> Generating certificate for $SERVER"

    # Generate server private key
    openssl genrsa -out $SERVER_DIR/${SERVER}-key.pem 2048

    # Generate certificate signing request
    openssl req -new -key $SERVER_DIR/${SERVER}-key.pem -out $SERVER_DIR/${SERVER}-csr.pem \
        -subj "/C=US/ST=State/L=City/O=Organization/OU=IT/CN=$SERVER"

    # Create SAN configuration
    cat > $SERVER_DIR/${SERVER}-san.cnf <<EOF
[req]
distinguished_name = req_distinguished_name
req_extensions = v3_req

[req_distinguished_name]

[v3_req]
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
subjectAltName = @alt_names

[alt_names]
DNS.1 = $SERVER
DNS.2 = ${SERVER%%.*}
DNS.3 = localhost
IP.1 = 127.0.0.1
EOF

    # Sign certificate with CA
    openssl x509 -req -in $SERVER_DIR/${SERVER}-csr.pem -CA $CA_DIR/ca-cert.pem \
        -CAkey $CA_DIR/ca-key.pem -CAcreateserial -out $SERVER_DIR/${SERVER}-cert.pem \
        -days 3650 -extensions v3_req -extfile $SERVER_DIR/${SERVER}-san.cnf

    # Verify certificate
    openssl verify -CAfile $CA_DIR/ca-cert.pem $SERVER_DIR/${SERVER}-cert.pem
done

echo "==> Creating combined certificate for HAProxy..."

# HAProxy needs cert + key in one file
cat $SERVER_DIR/${SERVER1}-cert.pem $SERVER_DIR/${SERVER1}-key.pem > $SERVER_DIR/haproxy-rabbitmq.pem

echo "==> Creating Java trust store..."

# Import CA certificate into Java KeyStore for batch processors
keytool -import -trustcacerts -alias rabbitmq-ca -file $CA_DIR/ca-cert.pem \
    -keystore $SERVER_DIR/rabbitmq-truststore.jks -storepass changeit -noprompt

echo ""
echo "==> Certificates generated successfully!"
echo ""
echo "CA Certificate:     $CA_DIR/ca-cert.pem"
echo "CA Key (KEEP SAFE): $CA_DIR/ca-key.pem"
echo ""
echo "Server Certificates:"
echo "  - $SERVER_DIR/${SERVER1}-cert.pem / ${SERVER1}-key.pem"
echo "  - $SERVER_DIR/${SERVER2}-cert.pem / ${SERVER2}-key.pem"
echo "  - $SERVER_DIR/${SERVER3}-cert.pem / ${SERVER3}-key.pem"
echo ""
echo "HAProxy Certificate: $SERVER_DIR/haproxy-rabbitmq.pem"
echo "Java TrustStore:     $SERVER_DIR/rabbitmq-truststore.jks (password: changeit)"
echo ""
echo "==> Next steps:"
echo "1. Copy files to Puppet module:"
echo "   sudo cp $CA_DIR/ca-cert.pem /etc/puppetlabs/code/environments/production/modules/fileprocessing/files/rabbitmq-ca-cert.pem"
echo "   sudo cp $SERVER_DIR/${SERVER1}-cert.pem /etc/puppetlabs/code/environments/production/modules/fileprocessing/files/rabbitmq-server-cert.pem"
echo "   sudo cp $SERVER_DIR/${SERVER1}-key.pem /etc/puppetlabs/code/environments/production/modules/fileprocessing/files/rabbitmq-server-key.pem"
echo "   sudo cp $SERVER_DIR/haproxy-rabbitmq.pem /etc/puppetlabs/code/environments/production/modules/fileprocessing/files/"
echo "   sudo cp $SERVER_DIR/rabbitmq-truststore.jks /etc/puppetlabs/code/environments/production/modules/fileprocessing/files/"
echo ""
echo "2. If you have multiple RabbitMQ servers with different hostnames,"
echo "   copy the appropriate server cert/key for each server as rabbitmq-server-cert.pem"
echo "   and rabbitmq-server-key.pem in the Puppet files directory."
echo ""
echo "3. Enable TLS in Hiera common.yaml:"
echo "   fileprocessing::enable_rabbitmq_tls: true"
echo "   fileprocessing::use_tls: true"
echo ""
echo "4. Deploy with Puppet!"
