#!/bin/bash
echo "Stopping HIE Development Environment..."
cd ~/Projects/hie-platform/docker-compose
docker-compose down
echo "Development environment stopped."
