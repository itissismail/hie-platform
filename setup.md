# HIE Development Environment Setup Guide - macOS #
## Phase 1: Foundation Tools Setup

### Step 1: Install Homebrew (Package Manager)
```
bash# Install Homebrew
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```
### Add Homebrew to PATH (add to ~/.zshrc or ~/.bash_profile)
```
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zshrc
source ~/.zshrc
```

### Verify installation
``` 
brew --version
Step 2: Install Development Tools
bash# Install Git
brew install git
``` 

### Install Java (OpenJDK 17 - LTS version)
```
brew install openjdk@17
 ```

### Add Java to PATH
```
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
echo 'export JAVA_HOME="/opt/homebrew/opt/openjdk@17"' >> ~/.zshrc
source ~/.zshrc
```

### Verify Java installation
```
java -version
javac -version
```

### Install Maven
```
brew install maven
```
### Install Node.js (for any frontend components)
```
brew install node
```

### Install Docker Desktop
```
brew install --cask docker
```

### Install Visual Studio Code
``` 
brew install --cask visual-studio-code 
```

### Install IntelliJ IDEA Community Edition
``` 
brew install --cask intellij-idea-ce 
```

### Step 3: Verify Base Installation
```
bash# Check all tools
git --version
java -version
mvn --version
node --version
docker --version
```
## Phase 2: Infrastructure Services Setup
### Step 4: Create Project Structure
```
bash# Create main project directory
mkdir -p ~/hie-platform
cd ~/hie-platform
```
### Create service directories
```
mkdir -p {api-gateway,validation-service,intake-service,conversion-service,storage-service}
mkdir -p {docker-compose,kubernetes,scripts,docs}
```
### Create infrastructure directory
```mkdir -p infrastructure/{rabbitmq,postgresql,redis,minio,monitoring}```
### Step 5: Docker Compose Infrastructure Setup
#### Create docker-compose.yml for infrastructure
```cd ~/hie-platform/docker-compose```
#### Create the following files:
``` 
docker-compose.yml
```
### Content
```
yamlversion: '3.8'

services:
# PostgreSQL Database
postgres:
image: postgres:15
container_name: hie-postgres
environment:
POSTGRES_DB: hie_platform
POSTGRES_USER: hie_user
POSTGRES_PASSWORD: hie_password
ports:
- "5432:5432"
volumes:
- postgres_data:/var/lib/postgresql/data
- ./init-scripts:/docker-entrypoint-initdb.d
networks:
- hie-network

# RabbitMQ Message Queue
rabbitmq:
image: rabbitmq:3-management
container_name: hie-rabbitmq
environment:
RABBITMQ_DEFAULT_USER: admin
RABBITMQ_DEFAULT_PASS: admin123
ports:
- "5672:5672"
- "15672:15672"
volumes:
- rabbitmq_data:/var/lib/rabbitmq
networks:
- hie-network

# Redis Cache
redis:
image: redis:7-alpine
container_name: hie-redis
ports:
- "6379:6379"
volumes:
- redis_data:/data
command: redis-server --appendonly yes
networks:
- hie-network

# MinIO Object Storage
minio:
image: minio/minio:latest
container_name: hie-minio
environment:
MINIO_ROOT_USER: minioadmin
MINIO_ROOT_PASSWORD: minioadmin123
ports:
- "9000:9000"
- "9001:9001"
volumes:
- minio_data:/data
command: server /data --console-address ":9001"
networks:
- hie-network

# Prometheus Monitoring
prometheus:
image: prom/prometheus:latest
container_name: hie-prometheus
ports:
- "9090:9090"
volumes:
- ./prometheus.yml:/etc/prometheus/prometheus.yml
- prometheus_data:/prometheus
networks:
- hie-network

# Grafana Dashboard
grafana:
image: grafana/grafana:latest
container_name: hie-grafana
environment:
GF_SECURITY_ADMIN_PASSWORD: admin123
ports:
- "3000:3000"
volumes:
- grafana_data:/var/lib/grafana
networks:
- hie-network

# Jaeger Tracing
jaeger:
image: jaegertracing/all-in-one:latest
container_name: hie-jaeger
ports:
- "16686:16686"
- "14268:14268"
environment:
COLLECTOR_OTLP_ENABLED: true
networks:
- hie-network

# Zipkin Tracing (Alternative)
zipkin:
image: openzipkin/zipkin:latest
container_name: hie-zipkin
ports:
- "9411:9411"
networks:
- hie-network

volumes:
postgres_data:
rabbitmq_data:
redis_data:
minio_data:
prometheus_data:
grafana_data:

networks:
hie-network:
driver: bridge
prometheus.yml
yamlglobal:
scrape_interval: 15s

scrape_configs:
- job_name: 'prometheus'
  static_configs:
    - targets: ['localhost:9090']

- job_name: 'hie-services'
  static_configs:
    - targets: ['host.docker.internal:8080', 'host.docker.internal:8081', 'host.docker.internal:8082']
      metrics_path: '/actuator/prometheus'
      Step 6: Database Initialization Scripts
      bash# Create init scripts directory
      mkdir -p ~/hie-platform/docker-compose/init-scripts
      init-scripts/01-create-tables.sql
      sql-- Create audit table
      CREATE TABLE message_audit (
      id BIGSERIAL PRIMARY KEY,
      message_id UUID NOT NULL,
      correlation_id UUID NOT NULL,
      service_name VARCHAR(50) NOT NULL,
      status VARCHAR(20) NOT NULL,
      processing_time_ms BIGINT,
      error_message TEXT,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      metadata JSONB
      );
```

## Create Database Tables
```
-- Create message state table
CREATE TABLE message_state (
id BIGSERIAL PRIMARY KEY,
message_id UUID UNIQUE NOT NULL,
current_status VARCHAR(20) NOT NULL,
source_organization VARCHAR(100) NOT NULL,
message_type VARCHAR(10) NOT NULL,
patient_id VARCHAR(50),
global_patient_id VARCHAR(50),
s3_location VARCHAR(500),
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_message_audit_correlation ON message_audit(correlation_id);
CREATE INDEX idx_message_audit_status ON message_audit(status);
CREATE INDEX idx_message_state_correlation ON message_state(message_id);
CREATE INDEX idx_message_state_status ON message_state(current_status);

-- Create quarantine table
CREATE TABLE quarantine_messages (
id BIGSERIAL PRIMARY KEY,
message_id UUID NOT NULL,
reason VARCHAR(255) NOT NULL,
raw_message TEXT NOT NULL,
quarantine_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
reviewed BOOLEAN DEFAULT FALSE,
reviewer VARCHAR(100),
review_date TIMESTAMP,
review_action VARCHAR(50)
);
```
## Phase 3: Start Infrastructure Services
### Step 7: Launch Infrastructure
```
bash# Navigate to docker-compose directory
cd ~/hie-platform/docker-compose
```
### Start all infrastructure services
```
docker-compose up -d
```

### Check if all services are running
```
docker-compose ps
```

### View logs if needed
```
docker-compose logs -f [service-name]
```
#### Step 8: Verify Infrastructure Services
```
bash# Test database connection
docker exec -it hie-postgres psql -U hie_user -d hie_platform -c "SELECT version();"

# Test RabbitMQ (visit http://localhost:15672, admin/admin123)
curl -u admin:admin123 http://localhost:15672/api/overview

# Test Redis
docker exec -it hie-redis redis-cli ping

# Test MinIO (visit http://localhost:9001, minioadmin/minioadmin123)
curl http://localhost:9000/minio/health/live

# Test Prometheus (visit http://localhost:9090)
curl http://localhost:9090/api/v1/status/config

# Test Grafana (visit http://localhost:3000, admin/admin123)
curl http://localhost:3000/api/health
```

## Phase 4: Development Tools Setup
### Step 9: IDE Configuration
```
bash# Install useful VS Code extensions
code --install-extension vscjava.vscode-java-pack
code --install-extension redhat.vscode-xml
code --install-extension ms-vscode.vscode-json
code --install-extension ms-python.python
code --install-extension ms-vscode.vscode-typescript-next
```
### Install IntelliJ IDEA plugins (manually through IDE)
#### - Spring Boot
#### - Database Navigator
#### - Docker
#### - Kubernetes

### Step 10: Create Development Scripts
```
bash# Create scripts directory
mkdir -p ~/hie-platform/scripts
```
### Create start script
```
cat > ~/hie-platform/scripts/start-dev.sh << 'EOF'
#!/bin/bash
echo "Starting HIE Development Environment..."

# Start infrastructure
cd ~/hie-platform/docker-compose
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
EOF

chmod +x ~/hie-platform/scripts/start-dev.sh
```

### Create stop script
```
cat > ~/hie-platform/scripts/stop-dev.sh << 'EOF'
#!/bin/bash
echo "Stopping HIE Development Environment..."
cd ~/hie-platform/docker-compose
docker-compose down
echo "Development environment stopped."
EOF
```
``` 
chmod +x ~/hie-platform/scripts/stop-dev.sh
```
## Phase 5: Java Service Templates
### Step 11: Create Service Templates
```
bash# Create API Gateway service
cd ~/hie-platform/api-gateway
mvn archetype:generate -DgroupId=com.hie.platform -DartifactId=api-gateway -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
```

### Create other services
```
cd ~/hie-platform/validation-service
mvn archetype:generate -DgroupId=com.hie.platform -DartifactId=validation-service -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false

cd ~/hie-platform/intake-service
mvn archetype:generate -DgroupId=com.hie.platform -DartifactId=intake-service -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
```
### Testing Your Setup
### Step 12: Test Infrastructure
```
bash# Run the start script
~/hie-platform/scripts/start-dev.sh

# Test RabbitMQ connectivity
curl -u admin:admin123 http://localhost:15672/api/queues

# Test PostgreSQL connectivity
docker exec -it hie-postgres psql -U hie_user -d hie_platform -c "\dt"

# Test MinIO
curl http://localhost:9000/minio/health/live
```
## What's Next?
### After completing this setup, you'll have:
#### ✅ Complete development infrastructure running in Docker
####✅ Database with proper schema
#### ✅ Message queue ready for use
#### ✅ Object storage for HL7 messages
#### ✅ Monitoring and observability stack
#### ✅ Development tools configured