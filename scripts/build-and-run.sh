#!/bin/bash

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# Move to project root directory
PROJECT_ROOT="$SCRIPT_DIR/.."
cd "$PROJECT_ROOT"

echo "Building all services..."

# Clean and install all modules
mvn clean install -DskipTests

# Build Docker images with OTel Java Agent
echo "Building Docker images with OpenTelemetry Java Agent..."
"$PROJECT_ROOT/scripts/build-with-agent.sh"

echo "Starting infrastructure services..."

# shellcheck disable=SC2164
cd "$PROJECT_ROOT/infrastructure/docker-compose"
docker compose up -d

echo "Waiting for MySQL to be ready..."
until docker exec docker-compose-mysql-1 mysql -uroot -proot -e "SELECT 1" > /dev/null 2>&1; do
    echo "Waiting for MySQL..."
    sleep 5
done

echo "Creating databases if not exists..."
docker exec docker-compose-mysql-1 mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS order; CREATE DATABASE IF NOT EXISTS product; CREATE DATABASE IF NOT EXISTS payment;"

echo "Running database migrations with Flyway..."
docker compose -f docker-compose-flyway.yml up
docker compose -f docker-compose-flyway.yml down

echo "Starting application services..."
docker compose -f docker-compose-services.yml up -d

echo "All services started!"
echo ""
echo "Access points:"
echo "- Order Service LB: http://localhost:8080"
echo "- Product Service LB: http://localhost:8090"
echo "- Payment Service LB: http://localhost:8100"
echo "- HAProxy Stats: http://localhost:9000/stats"
echo "- Kafka UI: http://localhost:28080"
echo "- Jaeger UI: http://localhost:16686"
echo ""
echo "Individual service instances:"
echo "- Order Service: 8181, 8281, 8381"
echo "- Product Service: 8182, 8282, 8382"
echo "- Payment Service: 8183, 8283, 8383"