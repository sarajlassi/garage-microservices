# 🚀 Garage Microservices - Configuration & Deployment Guide

## ✅ Project Structure Overview

```
garage-microservices/
├── docker-compose.yml                 # All services orchestration
├── pom.xml                           # Parent POM with 3 modules
├── auth-service/                      # Authentication & User Management
│   ├── pom.xml
│   ├── Dockerfile
│   ├── src/main/resources/application.yml
│   └── src/main/java/com/garage/auth/
├── vehicle-service/                   # Vehicle & Service Management
│   ├── pom.xml
│   ├── Dockerfile
│   ├── src/main/resources/application.yml
│   └── src/main/java/com/garage/vehicle/
└── stock-service/                     # Inventory & Stock Management (NEW)
    ├── pom.xml
    ├── Dockerfile
    ├── src/main/resources/application.yml
    └── src/main/java/com/garage/stock/

```

## 🔧 Configurations Fixed/Added

### 1. Auth Service Configuration ✅
**File**: `auth-service/src/main/resources/application.yml`
- **Bug Fixed**: Database credentials corrected
  - ❌ Old: username=admin, password=admin0000, url=jdbc:postgresql://localhost:5432/authDB
  - ✅ New: username=postgres, password=postgres, url=jdbc:postgresql://localhost:5432/auth_db

### 2. Vehicle Service Configuration ✅
**File**: `vehicle-service/src/main/resources/application.yml`
- **Added**: Consumer configuration for `product-low-stock` topic (from stock-service)

### 3. Stock Service Configuration NEW ✅
**File**: `stock-service/src/main/resources/application.yml`
- Port: 8083
- Database: stock_db (PostgreSQL on port 5434)
- Topics:
  - **Produces**: product-added, product-updated, product-low-stock, stock-reserved, supplier-order-placed
  - **Consumes**: service-scheduled (from vehicle-service)

### 4. Docker Compose Configuration ✅
**File**: `docker-compose.yml`
- **Added**:
  - `stock-postgres` service (port 5434)
  - `stock-service` container (port 8083)
- **Updated**: `volumes` section with `stock-postgres-data`

### 5. Parent POM Configuration ✅
**File**: `pom.xml`
- **Added**: `<module>stock-service</module>`

## 📦 Stock Service Components Created

### Entities (src/main/java/com/garage/stock/entity/)
- ✅ `Product.java` - Product catalog
- ✅ `Stock.java` - Current stock levels with thresholds
- ✅ `StockHistory.java` - Stock movement audit trail
- ✅ `SupplierOrder.java` - Supplier orders tracking

### Repositories (src/main/java/com/garage/stock/repository/)
- ✅ `ProductRepository.java`
- ✅ `StockRepository.java`
- ✅ `StockHistoryRepository.java`
- ✅ `SupplierOrderRepository.java`

### Services (src/main/java/com/garage/stock/service/)
- ✅ `ProductService.java` - Product CRUD operations
- ✅ `StockService.java` - Stock management (add, remove, reserve, check low stock)
- ✅ `SupplierOrderService.java` - Supplier order management

### Controllers (src/main/java/com/garage/stock/controller/)
- ✅ `ProductController.java` - REST endpoints for products
- ✅ `StockController.java` - REST endpoints for stock operations
- ✅ `SupplierOrderController.java` - REST endpoints for supplier orders

### Kafka (src/main/java/com/garage/stock/kafka/)
- ✅ `KafkaEvents.java` - Event DTOs (ProductAddedEvent, ProductLowStockEvent, StockReservedEvent, SupplierOrderPlacedEvent)
- ✅ `StockKafkaProducer.java` - Publishes stock events
- ✅ `StockKafkaConsumer.java` - Consumes service-scheduled events from vehicle-service

### Security (src/main/java/com/garage/stock/security/)
- ✅ `JwtUtil.java` - JWT token validation and extraction
- ✅ `JwtAuthenticationFilter.java` - Request authentication filter

### Configuration (src/main/java/com/garage/stock/config/)
- ✅ `SecurityConfig.java` - Spring Security configuration
- ✅ `KafkaConfig.java` - Kafka listener configuration
- ✅ `GlobalExceptionHandler.java` - Global error handling

### DTOs (src/main/java/com/garage/stock/dto/)
- ✅ `ProductDto.java`
- ✅ `StockDto.java`
- ✅ `SupplierOrderDto.java`

### Application
- ✅ `StockServiceApplication.java` - Spring Boot main class
- ✅ `application.yml` - Service configuration
- ✅ `Dockerfile` - Docker containerization
- ✅ `pom.xml` - Maven dependencies

## 🌐 Communication Flow

### Request Flow:
1. **Client** authenticates via `auth-service` → gets JWT token
2. **Client** calls `vehicle-service` with JWT → reserves vehicle
3. **Client** schedules a service → vehicle-service publishes `ServiceScheduledEvent`
4. **Stock-service** consumes event → checks required parts by service type
5. **Stock-service** publishes `ProductLowStockEvent` if stock insufficient
6. **Vehicle-service** receives alert → notifies mechanic
7. **Manager** can place supplier order via `stock-service`
8. When order arrives → stock quantity updated
9. When service completes → stock is deducted with audit trail

## 🚀 Deployment Instructions

### Option 1: Full Docker Deployment
```bash
cd garage-microservices
docker-compose up --build
```

Services available:
- Auth Service: http://localhost:8081
- Vehicle Service: http://localhost:8082
- Stock Service: http://localhost:8083
- Kafka UI: http://localhost:8090

### Option 2: Local Development
```bash
# Terminal 1: Start infrastructure
docker-compose up auth-postgres vehicle-postgres stock-postgres zookeeper kafka kafka-ui -d

# Terminal 2: Start auth-service
cd auth-service
mvn spring-boot:run

# Terminal 3: Start vehicle-service
cd vehicle-service
mvn spring-boot:run

# Terminal 4: Start stock-service
cd stock-service
mvn spring-boot:run
```

### Option 3: Build Only (for CI/CD)
```bash
# Build all services
mvn clean package -DskipTests

# Build individual services for Docker
cd auth-service && docker build -t auth-service:1.0.0 .
cd ../vehicle-service && docker build -t vehicle-service:1.0.0 .
cd ../stock-service && docker build -t stock-service:1.0.0 .
```

## 📋 Stock Management Endpoints

### Initialize Stock
```bash
curl -X POST "http://localhost:8083/api/stock/initialize?productId=1&initialQuantity=50&minThreshold=5&maxThreshold=100" \
  -H "Authorization: Bearer <token>"
```

### Add Stock
```bash
curl -X POST "http://localhost:8083/api/stock/1/add?quantity=20&reference=Delivery" \
  -H "Authorization: Bearer <token>"
```

### Reserve Stock (when service scheduled)
```bash
curl -X POST "http://localhost:8083/api/stock/1/reserve?quantity=1&serviceRecordId=100&vehicleId=50" \
  -H "Authorization: Bearer <token>"
```

### Place Supplier Order
```bash
curl -X POST "http://localhost:8083/api/stock/orders?productId=1&quantity=50&unitPrice=35.00&supplier=Shell&expectedDeliveryDate=2024-02-20" \
  -H "Authorization: Bearer <token>"
```

### Check Low Stock
```bash
curl "http://localhost:8083/api/stock/low-stock" \
  -H "Authorization: Bearer <token>"
```

## 🔍 Kafka Topics Overview

| Topic | Producer | Consumer | Message Type |
|-------|----------|----------|--------------|
| user-registered | auth-service | vehicle-service | UserRegisteredEvent |
| vehicle-created | vehicle-service | — | VehicleCreatedEvent |
| service-scheduled | vehicle-service | stock-service | ServiceScheduledEvent |
| product-low-stock | stock-service | vehicle-service | ProductLowStockEvent |
| stock-reserved | stock-service | — | StockReservedEvent |
| supplier-order-placed | stock-service | — | SupplierOrderPlacedEvent |

## 🛡️ Security

All services use JWT authentication. Each request requires:
```
Authorization: Bearer <JWT_TOKEN>
```

JWT Secret (same for all services):
```
404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
```

User Roles:
- USER: Basic access
- MECHANIC: Full vehicle & stock access
- ADMIN: Full system access

## ✨ Features Implemented

### Auth Service
- [x] User registration with role assignment
- [x] JWT authentication
- [x] Token refresh mechanism
- [x] Token validation endpoint

### Vehicle Service
- [x] Vehicle registration and management
- [x] Service scheduling
- [x] Service history tracking
- [x] Listens to user-registered events
- [x] Publishes service-scheduled events
- [x] Listens to low-stock alerts

### Stock Service (NEW)
- [x] Product catalog management
- [x] Stock level tracking
- [x] Stock threshold alerts
- [x] Stock history/audit trail
- [x] Supplier order management
- [x] Stock reservation
- [x] Automatic low-stock detection
- [x] Service-based stock allocation
- [x] Kafka event publishing
- [x] Kafka event consumption

## 📝 Next Steps

1. **Development**:
   - Add unit tests for all services
   - Add integration tests with testcontainers
   - Add API documentation with Swagger/SpringFox

2. **Enhancement**:
   - Add notification service
   - Add reporting/analytics
   - Add inventory forecasting
   - Add multi-warehouse support

3. **DevOps**:
   - Add Kubernetes manifests
   - Add CI/CD pipeline (GitHub Actions)
   - Add monitoring with Prometheus/Grafana
   - Add logging with ELK stack

## 🤝 Support

For issues or questions about the configuration, refer to:
- README_UPDATED.md - Complete API documentation
- application.yml files - Detailed configuration
- KafkaEvents.java files - Event schemas

