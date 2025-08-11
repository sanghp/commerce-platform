#!/bin/bash

echo "Building Docker images with OpenTelemetry Java Agent..."

# Order Service with Agent
docker build -t order-service-with-agent:latest -f docker/Dockerfile.order-agent .

# Product Service with Agent
docker build -t product-service-with-agent:latest -f docker/Dockerfile.product-agent .

# Payment Service with Agent
docker build -t payment-service-with-agent:latest -f docker/Dockerfile.payment-agent .

echo "Done! Now update docker-compose-services.yml to use the new images"