#!/bin/bash
echo "Starting HIE Development Environment..."

# Start infrastructure
cd ~/Projects/hie-platform/docker-compose
docker-compose up -d

# Wait for services to be ready
echo "Waiting for services to start..."
sleep 30

# Check service health
echo "Checking service health..."
docker-compose ps

echo "Development environment is ready!"
echo "RabbitMQ Management: http://localhost:15672 (admin/admin123)"
echo "MinIO Console: http://localhost:9001 (minioadmin/minioadmin123)"
echo "Grafana: http://localhost:3000 (admin/admin123)"
echo "Prometheus: http://localhost:9090"
echo "Jaeger: http://localhost:16686"
