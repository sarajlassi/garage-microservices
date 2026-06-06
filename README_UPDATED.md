# 🚗 Garage Microservices

A Spring Boot microservices project for managing a garage, consisting of:
- **auth-service** — JWT authentication, user registration & login
- **vehicle-service** — Vehicle & service record management
- **stock-service** — Inventory management, product stock, and supplier orders

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT                               │
└───────────────┬────────────────────┬────────────────────┬───┘
                │                    │                    │
      POST /api/auth/*    GET/POST /api/vehicles/*  GET/POST /api/stock/*
                │                    │                    │
   ┌────────────▼──────┐  ┌──────────▼──────────┐  ┌──────▼──────────┐
   │   auth-service    │  │ vehicle-service     │  │  stock-service  │
   │   :8081           │  │ :8082               │  │  :8083          │
   │                   │  │                     │  │                 │
   │  ┌────────────┐   │  │  ┌───────────────┐ │  │  ┌────────────┐ │
   │  │  auth_db   │   │  │  │  vehicle_db   │ │  │  │  stock_db  │ │
   │  │(postgres)  │   │  │  │  (postgres)   │ │  │  │ (postgres) │ │
   │  └────────────┘   │  │  └───────────────┘ │  │  └────────────┘ │
   └──────────────────┘  └─────────────────────┘  └─────────────────┘
                │                    │                    │
                └────────────────────┼────────────────────┘
                                     │
                        ┌────────────▼────────────┐
                        │  Apache Kafka           │
                        │  :9092                  │
                        │                         │
                        │  Topics:                │
                        │  user-registered       │
                        │  user-login            │
                        │  token-validated       │
                        │  vehicle-created       │
                        │  vehicle-updated       │
                        │  vehicle-deleted       │
                        │  service-scheduled     │
                        │  product-added         │
                        │  product-low-stock     │
                        │  stock-reserved        │
                        │  supplier-order-placed │
                        └─────────────────────────┘
```

## Kafka Event Flow

| Producer        | Topic                   | Consumer        | Purpose                          |
|-----------------|-------------------------|-----------------|----------------------------------|
| auth-service    | `user-registered`       | vehicle-service | Notify garage of new customer    |
| auth-service    | `user-login`            | —               | Audit trail                      |
| auth-service    | `token-validated`       | —               | Monitoring                       |
| vehicle-service | `vehicle-created`       | —               | Notifications/analytics          |
| vehicle-service | `vehicle-updated`       | —               | Status change tracking           |
| vehicle-service | `vehicle-deleted`       | —               | Cleanup tasks                    |
| vehicle-service | `service-scheduled`     | stock-service   | Stock allocation warnings        |
| stock-service   | `product-added`         | —               | Inventory updates                |
| stock-service   | `product-low-stock`     | vehicle-service | Alert mechanics of shortage      |
| stock-service   | `stock-reserved`        | —               | Reservation tracking             |
| stock-service   | `supplier-order-placed` | —               | Order confirmations              |

## Tech Stack

| Layer        | Technology                          |
|--------------|-------------------------------------|
| Framework    | Spring Boot 3.2, Spring Security 6  |
| Auth         | JWT (JJWT 0.11.5), BCrypt           |
| Database     | PostgreSQL 15, Spring Data JPA      |
| Messaging    | Apache Kafka (Confluent 7.5)        |
| Build        | Maven, Java 17                      |
| Container    | Docker, Docker Compose              |

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 17+, Maven 3.9+

### Run everything with Docker

```bash
docker-compose up --build
```

Services will be available at:
- Auth Service:    http://localhost:8081
- Vehicle Service: http://localhost:8082
- Stock Service:   http://localhost:8083
- Kafka UI:        http://localhost:8090

### Run locally (without Docker)

1. Start Postgres & Kafka:
```bash
docker-compose up auth-postgres vehicle-postgres stock-postgres zookeeper kafka kafka-ui -d
```

2. Start auth-service:
```bash
cd auth-service
mvn spring-boot:run
```

3. Start vehicle-service:
```bash
cd vehicle-service
mvn spring-boot:run
```

4. Start stock-service:
```bash
cd stock-service
mvn spring-boot:run
```

---

## API Reference

### Auth Service (port 8081)

#### Register a new user
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "secret123",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER"        // USER | ADMIN | MECHANIC
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "secret123"
}
```
Response:
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "userId": 1,
  "username": "john_doe",
  "role": "USER"
}
```

#### Refresh token
```http
POST /api/auth/refresh
Authorization: Bearer <refresh_token>
```

#### Validate token
```http
POST /api/auth/validate
Content-Type: application/json

{ "token": "eyJhbGci..." }
```

#### List all users (ADMIN only)
```http
GET /api/auth/users
Authorization: Bearer <token>
```

---

### Vehicle Service (port 8082)

All endpoints require: `Authorization: Bearer <token>`

#### Register a vehicle
```http
POST /api/vehicles
Authorization: Bearer <token>
X-User-Id: 1
Content-Type: application/json

{
  "licensePlate": "AB-123-CD",
  "make": "Renault",
  "model": "Clio",
  "year": 2020,
  "color": "Red",
  "vin": "VF1RFB00012345678",
  "mileage": 45000
}
```

#### Get all vehicles (ADMIN/MECHANIC only)
```http
GET /api/vehicles
Authorization: Bearer <token>
```

#### Get vehicle by ID
```http
GET /api/vehicles/{id}
Authorization: Bearer <token>
```

#### Get vehicles by owner
```http
GET /api/vehicles/owner/{ownerId}
Authorization: Bearer <token>
```

#### Get vehicles by status
```http
GET /api/vehicles/status/IN_SERVICE
Authorization: Bearer <token>
```
Statuses: `ACTIVE`, `IN_SERVICE`, `AWAITING_PARTS`, `REPAIRED`, `INACTIVE`, `SCRAPPED`

#### Update vehicle
```http
PUT /api/vehicles/{id}
Authorization: Bearer <token>

{
  "color": "Blue",
  "mileage": 50000,
  "status": "ACTIVE"
}
```

#### Delete vehicle (ADMIN only)
```http
DELETE /api/vehicles/{id}
Authorization: Bearer <token>
```

#### Schedule a service
```http
POST /api/vehicles/{vehicleId}/services
Authorization: Bearer <token>

{
  "description": "Full oil change and filter replacement",
  "serviceType": "OIL_CHANGE",
  "serviceDate": "2024-02-15",
  "scheduledDate": "2024-02-15",
  "mileageAtService": 50000,
  "cost": 89.99
}
```
Service types: `OIL_CHANGE`, `TIRE_ROTATION`, `BRAKE_SERVICE`, `ENGINE_REPAIR`,
`TRANSMISSION_SERVICE`, `AIR_FILTER_REPLACEMENT`, `BATTERY_REPLACEMENT`,
`COOLANT_FLUSH`, `WHEEL_ALIGNMENT`, `INSPECTION`, `GENERAL_MAINTENANCE`, `BODYWORK`, `OTHER`

#### Update service record
```http
PUT /api/vehicles/{vehicleId}/services/{recordId}
Authorization: Bearer <token>

{
  "serviceStatus": "COMPLETED",
  "completedDate": "2024-02-15",
  "cost": 95.00,
  "notes": "Also replaced air filter"
}
```
Service statuses: `SCHEDULED`, `IN_PROGRESS`, `AWAITING_PARTS`, `COMPLETED`, `CANCELLED`

#### Get service history
```http
GET /api/vehicles/{vehicleId}/services
Authorization: Bearer <token>
```

---

### Stock Service (port 8083)

All endpoints require: `Authorization: Bearer <token>`

#### Create a product
```http
POST /api/stock/products
Authorization: Bearer <token>
Content-Type: application/json

{
  "code": "OIL-5W30",
  "name": "Synthetic Oil 5W30",
  "description": "Premium synthetic motor oil",
  "unitPrice": 45.99,
  "category": "OIL",
  "supplier": "Shell",
  "sku": "SKU-12345"
}
```

#### Get all products
```http
GET /api/stock/products
Authorization: Bearer <token>
```

#### Get products by category
```http
GET /api/stock/products/category/{category}
Authorization: Bearer <token>
```
Categories: `OIL`, `FILTER`, `BRAKE_PAD`, `BATTERY`, `COOLANT`, `AIR_FILTER`, `TIRE`, `BRAKE_FLUID`, etc.

#### Initialize stock for a product
```http
POST /api/stock/initialize
Authorization: Bearer <token>

?productId=1&initialQuantity=50&minThreshold=5&maxThreshold=100
```

#### Add stock
```http
POST /api/stock/{productId}/add
Authorization: Bearer <token>

?quantity=20&reference=Supplier delivery
```

#### Remove stock (use for service)
```http
POST /api/stock/{productId}/remove
Authorization: Bearer <token>

?quantity=1&reference=Service-100&mechanicId=5
```

#### Reserve stock (for scheduled service)
```http
POST /api/stock/{productId}/reserve
Authorization: Bearer <token>

?quantity=2&serviceRecordId=50&vehicleId=10
```

#### Get low stock items
```http
GET /api/stock/low-stock
Authorization: Bearer <token>
```

#### Get stock for a product
```http
GET /api/stock/{productId}
Authorization: Bearer <token>
```

#### Get all stock
```http
GET /api/stock
Authorization: Bearer <token>
```

#### Place supplier order
```http
POST /api/stock/orders
Authorization: Bearer <token>

?productId=1&quantity=50&unitPrice=35.00&supplier=Shell&expectedDeliveryDate=2024-02-20
```

#### Get pending orders
```http
GET /api/stock/orders/pending
Authorization: Bearer <token>
```

#### Update order status
```http
PUT /api/stock/orders/{orderId}/status
Authorization: Bearer <token>

?status=ORDERED
```

#### Receive order (mark as received)
```http
PUT /api/stock/orders/{orderId}/receive
Authorization: Bearer <token>
```

---

## User Roles

| Role     | Capabilities                                           |
|----------|--------------------------------------------------------|
| USER     | Register/view own vehicles, view service history       |
| MECHANIC | All of USER + view all vehicles, create/update services, manage stock |
| ADMIN    | Full access including delete                           |

## Database Schema

### auth_db
- `users` — id, username, email, password, first_name, last_name, role, enabled, created_at
- `tokens` — id, token, token_type, expired, revoked, user_id, created_at

### vehicle_db
- `vehicles` — id, license_plate, make, model, year, color, vin, owner_id, status, mileage, last_service_date, notes
- `service_records` — id, vehicle_id, description, service_type, service_date, scheduled_date, completed_date, service_status, mechanic_id, cost, mileage_at_service, notes

### stock_db
- `products` — id, code, name, description, unit_price, category, supplier, sku, active, created_at, updated_at
- `stock` — id, product_id, quantity, min_threshold, max_threshold, reserved, created_at, last_restock_date, updated_at
- `stock_history` — id, product_id, stock_id, movement_type, quantity, reference, notes, mechanic_id, created_at
- `supplier_orders` — id, product_id, quantity, unit_price, total_price, supplier, status, order_date, expected_delivery_date, actual_delivery_date, reference_number, notes, created_at, updated_at

## Stock Management Flow

1. **Mechanic schedules a service** → `ServiceScheduledEvent` published
2. **Stock Service receives event** → Checks for required parts based on service type
3. **Low stock alert triggered** → `ProductLowStockEvent` published to vehicle-service
4. **Stock available** → Mechanic reserves the part
5. **Part is insufficient** → Manager can place a supplier order
6. **Order received** → Stock quantity updated automatically
7. **Service completed** → Stock is deducted from inventory with history tracking

