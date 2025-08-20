#!/bin/bash

echo "Manage HIE Services..."
# Go to Docker compose Directory
cd ../docker-compose/

case "$1" in
  buildStart)
    echo "Build and Start HIE services..."

    echo "Stop services..."
    docker-compose -f docker-compose-hie.yml down
    echo "Building services..."
    cd ../hie-services/message-router/
    docker build -t hie-message-router -f Dockerfile .
    cd ../validation-service/
    docker build -t hie-validation-service -f Dockerfile .
    cd ../auth-service/
    docker build -t hie-auth-service -f Dockerfile .
    cd ../api-gateway/
    docker build -t hie-api-gateway -f Dockerfile .
    echo "starting services..."
    cd ../../docker-compose/
    docker-compose -f docker-compose-hie.yml up -d
    ;;
  start)
    echo "Starting HIE services..."
    docker-compose -f docker-compose-hie.yml up -d
    ;;
  stop)
    echo "Stopping HIE services..."
    docker-compose -f docker-compose-hie.yml down
    ;;
  restart)
    echo "Restarting HIE services..."
    docker-compose -f docker-compose-hie.yml down
    docker-compose -f docker-compose-hie.yml up -d
    ;;
  logs)
    echo "Showing logs..."
    docker-compose logs -f
    ;;
  *)
    echo "Usage: $0 {start|stop|restart|logs|buildStart}"
    exit 1
esac
