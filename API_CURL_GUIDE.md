# API CURL Guide - Garage Microservices

## Base URLs
```
Auth Service: http://localhost:8081
Vehicle Service: http://localhost:8082
Stock Service: http://localhost:8083
```

---

## 1. AUTHENTICATION SERVICE (Port 8081)

### 1.1 Register User
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "mechanic1",
    "email": "mechanic1@garage.com",
    "password": "password123",
    "firstName": "Jean",
    "lastName": "Dupont",
    "role": "MECHANIC"
  }'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "username": "mechanic1",
  "email": "mechanic1@garage.com",
  "role": "MECHANIC"
}
```

### 1.2 Register Another User (Client)
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "client1",
    "email": "client1@email.com",
    "password": "password123",
    "firstName": "Pierre",
    "lastName": "Martin",
    "role": "USER"
  }'
```

### 1.3 Register Admin User
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "email": "admin@garage.com",
    "password": "admin123",
    "firstName": "Admin",
    "lastName": "Root",
    "role": "ADMIN"
  }'
```

### 1.4 Login User
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "mechanic1",
    "password": "password123"
  }'
```

**Response:** (Save the accessToken and userId for further requests)
```json
{
  "accessToken": "Bearer_TOKEN_HERE",
  "refreshToken": "REFRESH_TOKEN_HERE",
  "tokenType": "Bearer",
  "userId": 1,
  "username": "mechanic1",
  "email": "mechanic1@garage.com",
  "role": "MECHANIC"
}
```

### 1.5 Login Client
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "client1",
    "password": "password123"
  }'
```

### 1.6 Refresh Token
```bash
curl -X POST http://localhost:8081/api/auth/refresh \
  -H "Authorization: Bearer YOUR_REFRESH_TOKEN"
```

### 1.7 Validate Token
```bash
curl -X POST http://localhost:8081/api/auth/validate \
  -H "Content-Type: application/json" \
  -d '{
    "token": "YOUR_ACCESS_TOKEN"
  }'
```

### 1.8 Get All Users (Admin only)
```bash
curl -X GET http://localhost:8081/api/auth/users \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

---

## 2. VEHICLE SERVICE (Port 8082)

### Prerequisites
- Use the accessToken from Auth Service login
- Use the userId from Auth Service login

### 2.1 Create Vehicle
```bash
curl -X POST http://localhost:8082/api/vehicles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "X-User-Id: 2" \
  -d '{
    "licensePlate": "AB-123-CD",
    "make": "Peugeot",
    "model": "308",
    "year": 2022,
    "color": "Blue",
    "vin": "VF37JPHHD94U80819",
    "mileage": 15000,
    "notes": "New client vehicle"
  }'
```

**Response:**
```json
{
  "id": 1,
  "licensePlate": "AB-123-CD",
  "make": "Peugeot",
  "model": "308",
  "year": 2022,
  "color": "Blue",
  "vin": "VF37JPHHD94U80819",
  "ownerId": 2,
  "ownerUsername": "client1",
  "status": "ACTIVE",
  "mileage": 15000,
  "lastServiceDate": null,
  "notes": "New client vehicle",
  "createdAt": "2026-06-07T10:30:00",
  "updatedAt": "2026-06-07T10:30:00"
}
```

### 2.2 Create Another Vehicle
```bash
curl -X POST http://localhost:8082/api/vehicles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "X-User-Id: 2" \
  -d '{
    "licensePlate": "CD-456-EF",
    "make": "Renault",
    "model": "Clio",
    "year": 2021,
    "color": "Red",
    "vin": "VF1BH200G44532789",
    "mileage": 25000,
    "notes": "Regular maintenance"
  }'
```

### 2.3 Get Vehicle by ID
```bash
curl -X GET http://localhost:8082/api/vehicles/1 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 2.4 Get All Vehicles (Admin/Mechanic only)
```bash
curl -X GET http://localhost:8082/api/vehicles \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

### 2.5 Get Vehicles by Owner
```bash
curl -X GET http://localhost:8082/api/vehicles/owner/2 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 2.6 Get Vehicles by Status
```bash
curl -X GET "http://localhost:8082/api/vehicles/status/ACTIVE" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 2.7 Update Vehicle Status
```bash
curl -X PUT http://localhost:8082/api/vehicles/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "status": "IN_SERVICE",
    "mileage": 15100,
    "notes": "Engine inspection needed"
  }'
```

### 2.8 Update Vehicle Other Fields
```bash
curl -X PUT http://localhost:8082/api/vehicles/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "make": "Peugeot",
    "model": "308 SW",
    "color": "Dark Blue",
    "mileage": 15500,
    "notes": "Color changed, new mileage reading"
  }'
```

### 2.9 Delete Vehicle (Admin only)
```bash
curl -X DELETE http://localhost:8082/api/vehicles/1 \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

---

## 3. VEHICLE SERVICE RECORDS

### 3.1 Create Service Record (Start Repair)
```bash
curl -X POST http://localhost:8082/api/vehicles/1/services \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_MECHANIC_TOKEN" \
  -d '{
    "description": "Oil change and filter replacement",
    "serviceType": "MAINTENANCE",
    "serviceDate": "2026-06-07",
    "scheduledDate": "2026-06-07",
    "mileageAtService": 15500,
    "cost": 150.00,
    "notes": "Regular maintenance schedule"
  }'
```

**Response:**
```json
{
  "id": 1,
  "vehicleId": 1,
  "vehicleLicensePlate": "AB-123-CD",
  "description": "Oil change and filter replacement",
  "serviceType": "MAINTENANCE",
  "serviceDate": "2026-06-07",
  "scheduledDate": "2026-06-07",
  "completedDate": null,
  "serviceStatus": "SCHEDULED",
  "mechanicId": null,
  "mechanicUsername": "mechanic1",
  "cost": 150.00,
  "mileageAtService": 15500,
  "notes": "Regular maintenance schedule",
  "createdAt": "2026-06-07T11:00:00"
}
```

### 3.2 Create Service Record for Repair
```bash
curl -X POST http://localhost:8082/api/vehicles/1/services \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_MECHANIC_TOKEN" \
  -d '{
    "description": "Engine malfunction - cylinder head gasket replacement",
    "serviceType": "REPAIR",
    "serviceDate": "2026-06-07",
    "scheduledDate": "2026-06-08",
    "mileageAtService": 15500,
    "cost": 850.00,
    "notes": "Overheating issue detected - needs cylinder head gasket"
  }'
```

### 3.3 Get Service Records for Vehicle
```bash
curl -X GET http://localhost:8082/api/vehicles/1/services \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 3.4 Update Service Record (Add Products Used and Mark as In Progress)
```bash
curl -X PUT http://localhost:8082/api/vehicles/1/services/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_MECHANIC_TOKEN" \
  -d '{
    "description": "Oil change and filter replacement - IN PROGRESS",
    "serviceStatus": "IN_PROGRESS",
    "cost": 150.00,
    "mileageAtService": 15500,
    "notes": "Using Castrol Edge oil and OEM filter"
  }'
```

### 3.5 Complete Service Record
```bash
curl -X PUT http://localhost:8082/api/vehicles/1/services/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_MECHANIC_TOKEN" \
  -d '{
    "serviceStatus": "COMPLETED",
    "completedDate": "2026-06-07",
    "cost": 150.00,
    "mileageAtService": 15500,
    "notes": "Service completed successfully"
  }'
```

---

## 4. STOCK SERVICE - Products (Port 8083)

### 4.1 Create Product
```bash
curl -X POST http://localhost:8083/api/stock/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "code": "OIL-CASTROL-5W30",
    "name": "Castrol Edge 5W-30 Oil",
    "description": "Synthetic motor oil for modern engines",
    "unitPrice": 45.50,
    "category": "OIL",
    "supplier": "Castrol",
    "sku": "CASTROL-5W-30-5L",
    "active": true
  }'
```

**Response:**
```json
{
  "id": 1,
  "code": "OIL-CASTROL-5W30",
  "name": "Castrol Edge 5W-30 Oil",
  "description": "Synthetic motor oil for modern engines",
  "unitPrice": 45.50,
  "category": "OIL",
  "supplier": "Castrol",
  "sku": "CASTROL-5W-30-5L",
  "active": true,
  "createdAt": "2026-06-07T12:00:00",
  "updatedAt": "2026-06-07T12:00:00"
}
```

### 4.2 Create Multiple Products
```bash
# Air Filter
curl -X POST http://localhost:8083/api/stock/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "code": "FILTER-AIR-PEUGEOT",
    "name": "Air Filter for Peugeot 308",
    "description": "OEM air filter replacement",
    "unitPrice": 25.00,
    "category": "FILTERS",
    "supplier": "Peugeot",
    "sku": "PEUGEOT-AIR-FILTER-1",
    "active": true
  }'

# Oil Filter
curl -X POST http://localhost:8083/api/stock/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "code": "FILTER-OIL-1",
    "name": "Oil Filter W7026",
    "description": "Universal oil filter",
    "unitPrice": 18.50,
    "category": "FILTERS",
    "supplier": "Mann-Filter",
    "sku": "W7026",
    "active": true
  }'

# Brake Pads
curl -X POST http://localhost:8083/api/stock/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "code": "BRAKE-PAD-FRONT",
    "name": "Brake Pads Set Front",
    "description": "Premium ceramic brake pads",
    "unitPrice": 85.00,
    "category": "BRAKES",
    "supplier": "Brembo",
    "sku": "BREMBO-FRONT-SET",
    "active": true
  }'

# Spark Plugs
curl -X POST http://localhost:8083/api/stock/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "code": "SPARK-PLUG-BOSCH",
    "name": "Bosch Spark Plug Set",
    "description": "Set of 4 spark plugs",
    "unitPrice": 35.00,
    "category": "ENGINE",
    "supplier": "Bosch",
    "sku": "BOSCH-SPARK-4",
    "active": true
  }'
```

### 4.3 Get Product by ID
```bash
curl -X GET http://localhost:8083/api/stock/products/1 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 4.4 Get Product by Code
```bash
curl -X GET http://localhost:8083/api/stock/products/code/OIL-CASTROL-5W30 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 4.5 Get All Products
```bash
curl -X GET http://localhost:8083/api/stock/products \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 4.6 Get Products by Category
```bash
curl -X GET http://localhost:8083/api/stock/products/category/OIL \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 4.7 Update Product
```bash
curl -X PUT http://localhost:8083/api/stock/products/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "code": "OIL-CASTROL-5W30",
    "name": "Castrol Edge 5W-30 Full Synthetic Oil",
    "description": "High performance synthetic motor oil",
    "unitPrice": 48.00,
    "category": "OIL",
    "supplier": "Castrol",
    "sku": "CASTROL-5W-30-5L",
    "active": true
  }'
```

### 4.8 Delete Product
```bash
curl -X DELETE http://localhost:8083/api/stock/products/1 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## 5. STOCK SERVICE - Stock Management

### 5.1 Initialize Stock for Product
```bash
curl -X POST "http://localhost:8083/api/stock/initialize?productId=1&initialQuantity=50&minThreshold=5&maxThreshold=100" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Response:**
```json
{
  "id": 1,
  "productId": 1,
  "productCode": "OIL-CASTROL-5W30",
  "productName": "Castrol Edge 5W-30 Oil",
  "currentQuantity": 50,
  "reservedQuantity": 0,
  "availableQuantity": 50,
  "minThreshold": 5,
  "maxThreshold": 100,
  "lastRestockDate": "2026-06-07T12:30:00",
  "nextRestockDate": null
}
```

### 5.2 Initialize Stock for Multiple Products
```bash
# Air Filter
curl -X POST "http://localhost:8083/api/stock/initialize?productId=2&initialQuantity=30&minThreshold=3&maxThreshold=50" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Oil Filter
curl -X POST "http://localhost:8083/api/stock/initialize?productId=3&initialQuantity=40&minThreshold=5&maxThreshold=80" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Brake Pads
curl -X POST "http://localhost:8083/api/stock/initialize?productId=4&initialQuantity=20&minThreshold=2&maxThreshold=30" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Spark Plugs
curl -X POST "http://localhost:8083/api/stock/initialize?productId=5&initialQuantity=25&minThreshold=3&maxThreshold=50" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 5.3 Add Stock (Restock)
```bash
curl -X POST "http://localhost:8083/api/stock/1/add?quantity=25&reference=Order-PO-2026-001" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 5.4 Remove Stock (Used for Repair)
```bash
curl -X POST "http://localhost:8083/api/stock/1/remove?quantity=1&reference=Service-ID-1&mechanicId=1" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 5.5 Reserve Stock for Service
```bash
curl -X POST "http://localhost:8083/api/stock/1/reserve?quantity=1&serviceRecordId=1&vehicleId=1" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 5.6 Get Stock for Product
```bash
curl -X GET http://localhost:8083/api/stock/1 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 5.7 Get Low Stock Items
```bash
curl -X GET http://localhost:8083/api/stock/low-stock \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 5.8 Get All Stock
```bash
curl -X GET http://localhost:8083/api/stock \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 5.9 Get Stock by Category
```bash
curl -X GET http://localhost:8083/api/stock/category/OIL \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## 6. SUPPLIER ORDERS (Product Orders from Fournisseur)

### 6.1 Place Order from Supplier
```bash
curl -X POST "http://localhost:8083/api/stock/orders?productId=1&quantity=100&unitPrice=40.00&supplier=Castrol&expectedDeliveryDate=2026-06-15" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Response:**
```json
{
  "id": 1,
  "productId": 1,
  "productCode": "OIL-CASTROL-5W30",
  "productName": "Castrol Edge 5W-30 Oil",
  "quantity": 100,
  "unitPrice": 40.00,
  "totalPrice": 4000.00,
  "supplier": "Castrol",
  "status": "PENDING",
  "orderDate": "2026-06-07T13:00:00",
  "expectedDeliveryDate": "2026-06-15",
  "actualDeliveryDate": null,
  "reference": "ORD-20260607-001"
}
```

### 6.2 Place Multiple Orders from Different Suppliers
```bash
# Order Air Filters
curl -X POST "http://localhost:8083/api/stock/orders?productId=2&quantity=50&unitPrice=20.00&supplier=Peugeot&expectedDeliveryDate=2026-06-14" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Order Oil Filters
curl -X POST "http://localhost:8083/api/stock/orders?productId=3&quantity=75&unitPrice=15.00&supplier=Mann-Filter&expectedDeliveryDate=2026-06-12" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Order Brake Pads
curl -X POST "http://localhost:8083/api/stock/orders?productId=4&quantity=40&unitPrice=75.00&supplier=Brembo&expectedDeliveryDate=2026-06-20" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Order Spark Plugs
curl -X POST "http://localhost:8083/api/stock/orders?productId=5&quantity=60&unitPrice=30.00&supplier=Bosch&expectedDeliveryDate=2026-06-10" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 6.3 Get Order by ID
```bash
curl -X GET http://localhost:8083/api/stock/orders/1 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 6.4 Get Orders by Product
```bash
curl -X GET http://localhost:8083/api/stock/orders/product/1 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 6.5 Get Orders by Status
```bash
# Get pending orders
curl -X GET "http://localhost:8083/api/stock/orders/status/PENDING" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Get delivered orders
curl -X GET "http://localhost:8083/api/stock/orders/status/DELIVERED" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 6.6 Get Pending Orders
```bash
curl -X GET http://localhost:8083/api/stock/orders/pending \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 6.7 Get Orders by Supplier
```bash
curl -X GET "http://localhost:8083/api/stock/orders/supplier/Castrol" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 6.8 Get All Orders
```bash
curl -X GET http://localhost:8083/api/stock/orders \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 6.9 Update Order Status (Mark as Confirmed/Processing)
```bash
curl -X PUT "http://localhost:8083/api/stock/orders/1/status?status=PROCESSING" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 6.10 Receive Order (Mark as Delivered)
```bash
curl -X PUT "http://localhost:8083/api/stock/orders/1/receive" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## Complete Workflow Example

### Step 1: Register Users
```bash
# Register Mechanic
MECHANIC_RESPONSE=$(curl -s -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "mechanic1",
    "email": "mechanic1@garage.com",
    "password": "password123",
    "firstName": "Jean",
    "lastName": "Dupont",
    "role": "MECHANIC"
  }')

# Extract userId and save it
MECHANIC_ID=$(echo $MECHANIC_RESPONSE | jq '.userId')
echo "Mechanic ID: $MECHANIC_ID"

# Register Client
CLIENT_RESPONSE=$(curl -s -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "client1",
    "email": "client1@email.com",
    "password": "password123",
    "firstName": "Pierre",
    "lastName": "Martin",
    "role": "USER"
  }')

CLIENT_ID=$(echo $CLIENT_RESPONSE | jq '.userId')
echo "Client ID: $CLIENT_ID"
```

### Step 2: Login and Get Tokens
```bash
# Login Mechanic
MECHANIC_LOGIN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "mechanic1",
    "password": "password123"
  }')

MECHANIC_TOKEN=$(echo $MECHANIC_LOGIN | jq -r '.accessToken')
echo "Mechanic Token: $MECHANIC_TOKEN"

# Login Client
CLIENT_LOGIN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "client1",
    "password": "password123"
  }')

CLIENT_TOKEN=$(echo $CLIENT_LOGIN | jq -r '.accessToken')
echo "Client Token: $CLIENT_TOKEN"
```

### Step 3: Create Vehicle
```bash
curl -X POST http://localhost:8082/api/vehicles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "X-User-Id: $CLIENT_ID" \
  -d '{
    "licensePlate": "AB-123-CD",
    "make": "Peugeot",
    "model": "308",
    "year": 2022,
    "color": "Blue",
    "vin": "VF37JPHHD94U80819",
    "mileage": 15000
  }'
```

### Step 4: Create Products in Stock
```bash
curl -X POST http://localhost:8083/api/stock/products \
  -H "Content-Type: application/json" \
  -d '{
    "code": "OIL-CASTROL-5W30",
    "name": "Castrol Edge 5W-30 Oil",
    "unitPrice": 45.50,
    "category": "OIL",
    "supplier": "Castrol",
    "active": true
  }'
```

### Step 5: Initialize Stock
```bash
curl -X POST "http://localhost:8083/api/stock/initialize?productId=1&initialQuantity=50" \
  -H "Authorization: Bearer $MECHANIC_TOKEN"
```

### Step 6: Create Service Record
```bash
curl -X POST http://localhost:8082/api/vehicles/1/services \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MECHANIC_TOKEN" \
  -d '{
    "description": "Oil change",
    "serviceType": "MAINTENANCE",
    "serviceDate": "2026-06-07",
    "cost": 150.00
  }'
```

### Step 7: Reserve Stock for Service
```bash
curl -X POST "http://localhost:8083/api/stock/1/reserve?quantity=1&serviceRecordId=1&vehicleId=1" \
  -H "Authorization: Bearer $MECHANIC_TOKEN"
```

### Step 8: Complete Service
```bash
curl -X PUT http://localhost:8082/api/vehicles/1/services/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MECHANIC_TOKEN" \
  -d '{
    "serviceStatus": "COMPLETED",
    "completedDate": "2026-06-07"
  }'
```

### Step 9: Order from Supplier (Out of Stock Alert)
```bash
curl -X POST "http://localhost:8083/api/stock/orders?productId=1&quantity=100&unitPrice=40.00&supplier=Castrol" \
  -H "Authorization: Bearer $MECHANIC_TOKEN"
```

### Step 10: Receive Order
```bash
curl -X PUT "http://localhost:8083/api/stock/orders/1/receive" \
  -H "Authorization: Bearer $MECHANIC_TOKEN"
```

---

## Notes

1. **Authorization Header Format**: `Authorization: Bearer YOUR_ACCESS_TOKEN`
2. **X-User-Id Header**: Required for vehicle creation (should be the customer's ID)
3. **Roles**: 
   - USER: Regular customer
   - MECHANIC: Can perform repairs and access vehicle data
   - ADMIN: Can perform all operations including user management
4. **Token Storage**: Save tokens to environment variables for easier use in scripts
5. **Kafka Integration**: All events are published to Kafka topics (user-registered, vehicle-created, service-scheduled, etc.)

## Error Handling

The APIs return appropriate HTTP status codes:
- 200: Success
- 201: Created
- 400: Bad Request
- 401: Unauthorized
- 403: Forbidden
- 404: Not Found
- 500: Internal Server Error

Response format for errors:
```json
{
  "timestamp": "2026-06-07T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Error description",
  "path": "/api/vehicles"
}
```

