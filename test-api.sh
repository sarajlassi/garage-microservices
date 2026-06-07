#!/bin/bash

# =====================================================
# Garage Microservices - API Test Script
# =====================================================

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
AUTH_BASE_URL="http://localhost:8081"
VEHICLE_BASE_URL="http://localhost:8082"
STOCK_BASE_URL="http://localhost:8083"

# Global variables to store tokens and IDs
MECHANIC_TOKEN=""
CLIENT_TOKEN=""
ADMIN_TOKEN=""
MECHANIC_ID=""
CLIENT_ID=""
VEHICLE_ID=""
SERVICE_ID=""
PRODUCT_ID=""
ORDER_ID=""

# =====================================================
# Helper Functions
# =====================================================

print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

save_token() {
    local token=$(echo $1 | jq -r '.accessToken // .token')
    local id=$(echo $1 | jq -r '.userId')
    echo $token
}

# =====================================================
# 1. AUTHENTICATION TESTS
# =====================================================

test_auth_register_mechanic() {
    print_header "1. Register Mechanic User"

    RESPONSE=$(curl -s -X POST $AUTH_BASE_URL/api/auth/register \
        -H "Content-Type: application/json" \
        -d '{
            "username": "mechanic1",
            "email": "mechanic1@garage.com",
            "password": "password123",
            "firstName": "Jean",
            "lastName": "Dupont",
            "role": "MECHANIC"
        }')

    echo "Response:"
    echo $RESPONSE | jq '.'

    MECHANIC_ID=$(echo $RESPONSE | jq -r '.userId')
    print_success "Mechanic registered with ID: $MECHANIC_ID"
}

test_auth_register_client() {
    print_header "2. Register Client User"

    RESPONSE=$(curl -s -X POST $AUTH_BASE_URL/api/auth/register \
        -H "Content-Type: application/json" \
        -d '{
            "username": "client1",
            "email": "client1@email.com",
            "password": "password123",
            "firstName": "Pierre",
            "lastName": "Martin",
            "role": "USER"
        }')

    echo "Response:"
    echo $RESPONSE | jq '.'

    CLIENT_ID=$(echo $RESPONSE | jq -r '.userId')
    print_success "Client registered with ID: $CLIENT_ID"
}

test_auth_register_admin() {
    print_header "3. Register Admin User"

    RESPONSE=$(curl -s -X POST $AUTH_BASE_URL/api/auth/register \
        -H "Content-Type: application/json" \
        -d '{
            "username": "admin",
            "email": "admin@garage.com",
            "password": "admin123",
            "firstName": "Admin",
            "lastName": "Root",
            "role": "ADMIN"
        }')

    echo "Response:"
    echo $RESPONSE | jq '.'
}

test_auth_login_mechanic() {
    print_header "4. Login Mechanic"

    RESPONSE=$(curl -s -X POST $AUTH_BASE_URL/api/auth/login \
        -H "Content-Type: application/json" \
        -d '{
            "username": "mechanic1",
            "password": "password123"
        }')

    echo "Response:"
    echo $RESPONSE | jq '.'

    MECHANIC_TOKEN=$(echo $RESPONSE | jq -r '.accessToken')
    print_success "Mechanic Token: ${MECHANIC_TOKEN:0:50}..."
}

test_auth_login_client() {
    print_header "5. Login Client"

    RESPONSE=$(curl -s -X POST $AUTH_BASE_URL/api/auth/login \
        -H "Content-Type: application/json" \
        -d '{
            "username": "client1",
            "password": "password123"
        }')

    echo "Response:"
    echo $RESPONSE | jq '.'

    CLIENT_TOKEN=$(echo $RESPONSE | jq -r '.accessToken')
    print_success "Client Token: ${CLIENT_TOKEN:0:50}..."
}

test_auth_validate_token() {
    print_header "6. Validate Token"

    RESPONSE=$(curl -s -X POST $AUTH_BASE_URL/api/auth/validate \
        -H "Content-Type: application/json" \
        -d "{\"token\": \"$MECHANIC_TOKEN\"}")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

# =====================================================
# 2. VEHICLE TESTS
# =====================================================

test_vehicle_create() {
    print_header "7. Create Vehicle"

    RESPONSE=$(curl -s -X POST $VEHICLE_BASE_URL/api/vehicles \
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
            "mileage": 15000,
            "notes": "New client vehicle"
        }')

    echo "Response:"
    echo $RESPONSE | jq '.'

    VEHICLE_ID=$(echo $RESPONSE | jq -r '.id')
    print_success "Vehicle created with ID: $VEHICLE_ID"
}

test_vehicle_get() {
    print_header "8. Get Vehicle by ID"

    RESPONSE=$(curl -s -X GET $VEHICLE_BASE_URL/api/vehicles/$VEHICLE_ID \
        -H "Authorization: Bearer $CLIENT_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

test_vehicle_get_all() {
    print_header "9. Get All Vehicles"

    RESPONSE=$(curl -s -X GET $VEHICLE_BASE_URL/api/vehicles \
        -H "Authorization: Bearer $MECHANIC_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

test_vehicle_update_status() {
    print_header "10. Update Vehicle Status to IN_SERVICE"

    RESPONSE=$(curl -s -X PUT $VEHICLE_BASE_URL/api/vehicles/$VEHICLE_ID \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $MECHANIC_TOKEN" \
        -d '{
            "status": "IN_SERVICE",
            "notes": "Vehicle received for service"
        }')

    echo "Response:"
    echo $RESPONSE | jq '.'
}

test_vehicle_update_fields() {
    print_header "11. Update Vehicle Fields"

    RESPONSE=$(curl -s -X PUT $VEHICLE_BASE_URL/api/vehicles/$VEHICLE_ID \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $MECHANIC_TOKEN" \
        -d '{
            "mileage": 15500,
            "color": "Dark Blue",
            "notes": "Updated color and mileage"
        }')

    echo "Response:"
    echo $RESPONSE | jq '.'
}

# =====================================================
# 3. SERVICE RECORDS TESTS
# =====================================================

test_service_create() {
    print_header "12. Create Service Record"

    RESPONSE=$(curl -s -X POST $VEHICLE_BASE_URL/api/vehicles/$VEHICLE_ID/services \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $MECHANIC_TOKEN" \
        -d '{
            "description": "Oil change and filter replacement",
            "serviceType": "MAINTENANCE",
            "serviceDate": "2026-06-07",
            "scheduledDate": "2026-06-07",
            "mileageAtService": 15500,
            "cost": 150.00,
            "notes": "Regular maintenance schedule"
        }')

    echo "Response:"
    echo $RESPONSE | jq '.'

    SERVICE_ID=$(echo $RESPONSE | jq -r '.id')
    print_success "Service Record created with ID: $SERVICE_ID"
}

test_service_get_list() {
    print_header "13. Get Service Records for Vehicle"

    RESPONSE=$(curl -s -X GET $VEHICLE_BASE_URL/api/vehicles/$VEHICLE_ID/services \
        -H "Authorization: Bearer $MECHANIC_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

test_service_update() {
    print_header "14. Update Service Record Status"

    RESPONSE=$(curl -s -X PUT $VEHICLE_BASE_URL/api/vehicles/$VEHICLE_ID/services/$SERVICE_ID \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $MECHANIC_TOKEN" \
        -d '{
            "serviceStatus": "IN_PROGRESS",
            "cost": 150.00,
            "notes": "Service in progress - using Castrol oil"
        }')

    echo "Response:"
    echo $RESPONSE | jq '.'
}

test_service_complete() {
    print_header "15. Complete Service Record"

    RESPONSE=$(curl -s -X PUT $VEHICLE_BASE_URL/api/vehicles/$VEHICLE_ID/services/$SERVICE_ID \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $MECHANIC_TOKEN" \
        -d '{
            "serviceStatus": "COMPLETED",
            "completedDate": "2026-06-07",
            "cost": 150.00,
            "mileageAtService": 15500
        }')

    echo "Response:"
    echo $RESPONSE | jq '.'
}

# =====================================================
# 4. PRODUCT TESTS
# =====================================================

test_product_create() {
    print_header "16. Create Product"

    RESPONSE=$(curl -s -X POST $STOCK_BASE_URL/api/stock/products \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $MECHANIC_TOKEN" \
        -d '{
            "code": "OIL-CASTROL-5W30",
            "name": "Castrol Edge 5W-30 Oil",
            "description": "Synthetic motor oil",
            "unitPrice": 45.50,
            "category": "OIL",
            "supplier": "Castrol",
            "sku": "CASTROL-5W-30-5L",
            "active": true
        }')

    echo "Response:"
    echo $RESPONSE | jq '.'

    PRODUCT_ID=$(echo $RESPONSE | jq -r '.id')
    print_success "Product created with ID: $PRODUCT_ID"
}

test_product_create_multiple() {
    print_header "17. Create Multiple Products"

    # Air Filter
    curl -s -X POST $STOCK_BASE_URL/api/stock/products \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $MECHANIC_TOKEN" \
        -d '{
            "code": "FILTER-AIR-PEUGEOT",
            "name": "Air Filter",
            "unitPrice": 25.00,
            "category": "FILTERS",
            "supplier": "Peugeot",
            "active": true
        }' | jq '.id'

    # Oil Filter
    curl -s -X POST $STOCK_BASE_URL/api/stock/products \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $MECHANIC_TOKEN" \
        -d '{
            "code": "FILTER-OIL",
            "name": "Oil Filter",
            "unitPrice": 18.50,
            "category": "FILTERS",
            "supplier": "Mann",
            "active": true
        }' | jq '.id'

    print_success "Multiple products created"
}

test_product_get() {
    print_header "18. Get Product by ID"

    RESPONSE=$(curl -s -X GET $STOCK_BASE_URL/api/stock/products/$PRODUCT_ID \
        -H "Authorization: Bearer $MECHANIC_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

test_product_get_all() {
    print_header "19. Get All Products"

    RESPONSE=$(curl -s -X GET $STOCK_BASE_URL/api/stock/products \
        -H "Authorization: Bearer $MECHANIC_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

test_product_get_by_category() {
    print_header "20. Get Products by Category"

    RESPONSE=$(curl -s -X GET $STOCK_BASE_URL/api/stock/products/category/OIL \
        -H "Authorization: Bearer $MECHANIC_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

# =====================================================
# 5. STOCK MANAGEMENT TESTS
# =====================================================

test_stock_initialize() {
    print_header "21. Initialize Stock"

    RESPONSE=$(curl -s -X POST "$STOCK_BASE_URL/api/stock/initialize?productId=$PRODUCT_ID&initialQuantity=50&minThreshold=5&maxThreshold=100" \
        -H "Authorization: Bearer $MECHANIC_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

test_stock_add() {
    print_header "22. Add Stock"

    RESPONSE=$(curl -s -X POST "$STOCK_BASE_URL/api/stock/$PRODUCT_ID/add?quantity=25&reference=Order-PO-001" \
        -H "Authorization: Bearer $MECHANIC_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

test_stock_remove() {
    print_header "23. Remove Stock for Repair"

    RESPONSE=$(curl -s -X POST "$STOCK_BASE_URL/api/stock/$PRODUCT_ID/remove?quantity=1&reference=Service-$SERVICE_ID&mechanicId=$MECHANIC_ID" \
        -H "Authorization: Bearer $MECHANIC_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

test_stock_reserve() {
    print_header "24. Reserve Stock for Service"

    RESPONSE=$(curl -s -X POST "$STOCK_BASE_URL/api/stock/$PRODUCT_ID/reserve?quantity=1&serviceRecordId=$SERVICE_ID&vehicleId=$VEHICLE_ID" \
        -H "Authorization: Bearer $MECHANIC_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

test_stock_get() {
    print_header "25. Get Stock for Product"

    RESPONSE=$(curl -s -X GET $STOCK_BASE_URL/api/stock/$PRODUCT_ID \
        -H "Authorization: Bearer $MECHANIC_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

test_stock_get_all() {
    print_header "26. Get All Stock"

    RESPONSE=$(curl -s -X GET $STOCK_BASE_URL/api/stock \
        -H "Authorization: Bearer $MECHANIC_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

test_stock_low_stock() {
    print_header "27. Get Low Stock Items"

    RESPONSE=$(curl -s -X GET $STOCK_BASE_URL/api/stock/low-stock \
        -H "Authorization: Bearer $MECHANIC_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

# =====================================================
# 6. SUPPLIER ORDERS TESTS
# =====================================================

test_order_place() {
    print_header "28. Place Order from Supplier"

    RESPONSE=$(curl -s -X POST "$STOCK_BASE_URL/api/stock/orders?productId=$PRODUCT_ID&quantity=100&unitPrice=40.00&supplier=Castrol&expectedDeliveryDate=2026-06-15" \
        -H "Authorization: Bearer $MECHANIC_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'

    ORDER_ID=$(echo $RESPONSE | jq -r '.id')
    print_success "Order placed with ID: $ORDER_ID"
}

test_order_get() {
    print_header "29. Get Order by ID"

    RESPONSE=$(curl -s -X GET $STOCK_BASE_URL/api/stock/orders/$ORDER_ID \
        -H "Authorization: Bearer $MECHANIC_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

test_order_get_pending() {
    print_header "30. Get Pending Orders"

    RESPONSE=$(curl -s -X GET $STOCK_BASE_URL/api/stock/orders/pending \
        -H "Authorization: Bearer $MECHANIC_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

test_order_get_all() {
    print_header "31. Get All Orders"

    RESPONSE=$(curl -s -X GET $STOCK_BASE_URL/api/stock/orders \
        -H "Authorization: Bearer $MECHANIC_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

test_order_update_status() {
    print_header "32. Update Order Status"

    RESPONSE=$(curl -s -X PUT "$STOCK_BASE_URL/api/stock/orders/$ORDER_ID/status?status=PROCESSING" \
        -H "Authorization: Bearer $MECHANIC_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

test_order_receive() {
    print_header "33. Receive Order"

    RESPONSE=$(curl -s -X PUT "$STOCK_BASE_URL/api/stock/orders/$ORDER_ID/receive" \
        -H "Authorization: Bearer $MECHANIC_TOKEN")

    echo "Response:"
    echo $RESPONSE | jq '.'
}

# =====================================================
# Main Menu
# =====================================================

show_menu() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Garage Microservices - API Test Menu${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo -e "${YELLOW}Authentication Tests:${NC}"
    echo "1. Test All Auth (Register + Login)"
    echo "2. Register Mechanic"
    echo "3. Register Client"
    echo "4. Register Admin"
    echo "5. Login Mechanic"
    echo "6. Login Client"
    echo "7. Validate Token"
    echo ""
    echo -e "${YELLOW}Vehicle Tests:${NC}"
    echo "8. Create Vehicle"
    echo "9. Get Vehicle"
    echo "10. Update Vehicle Status"
    echo "11. Update Vehicle Fields"
    echo ""
    echo -e "${YELLOW}Service Records Tests:${NC}"
    echo "12. Create Service Record"
    echo "13. Get Service Records"
    echo "14. Update Service Status"
    echo "15. Complete Service"
    echo ""
    echo -e "${YELLOW}Product Tests:${NC}"
    echo "16. Create Product"
    echo "17. Create Multiple Products"
    echo "18. Get Product"
    echo "19. Get All Products"
    echo "20. Get Products by Category"
    echo ""
    echo -e "${YELLOW}Stock Management Tests:${NC}"
    echo "21. Initialize Stock"
    echo "22. Add Stock"
    echo "23. Remove Stock"
    echo "24. Reserve Stock"
    echo "25. Get Stock"
    echo "26. Get All Stock"
    echo "27. Get Low Stock"
    echo ""
    echo -e "${YELLOW}Supplier Orders Tests:${NC}"
    echo "28. Place Order"
    echo "29. Get Order"
    echo "30. Get Pending Orders"
    echo "31. Get All Orders"
    echo "32. Update Order Status"
    echo "33. Receive Order"
    echo ""
    echo -e "${YELLOW}Complete Workflows:${NC}"
    echo "99. Run Complete Workflow"
    echo "0. Exit"
    echo ""
}

run_complete_workflow() {
    print_header "COMPLETE WORKFLOW TEST"

    test_auth_register_mechanic
    test_auth_register_client
    test_auth_login_mechanic
    test_auth_login_client
    test_vehicle_create
    test_product_create
    test_stock_initialize
    test_service_create
    test_stock_reserve
    test_service_complete
    test_order_place
    test_order_receive

    print_success "WORKFLOW COMPLETED!"
}

# =====================================================
# Main Script
# =====================================================

if [ $# -eq 0 ]; then
    # Interactive mode
    while true; do
        show_menu
        read -p "Enter your choice: " choice

        case $choice in
            1) test_auth_register_mechanic; test_auth_register_client; test_auth_login_mechanic; test_auth_login_client ;;
            2) test_auth_register_mechanic ;;
            3) test_auth_register_client ;;
            4) test_auth_register_admin ;;
            5) test_auth_login_mechanic ;;
            6) test_auth_login_client ;;
            7) test_auth_validate_token ;;
            8) test_vehicle_create ;;
            9) test_vehicle_get ;;
            10) test_vehicle_update_status ;;
            11) test_vehicle_update_fields ;;
            12) test_service_create ;;
            13) test_service_get_list ;;
            14) test_service_update ;;
            15) test_service_complete ;;
            16) test_product_create ;;
            17) test_product_create_multiple ;;
            18) test_product_get ;;
            19) test_product_get_all ;;
            20) test_product_get_by_category ;;
            21) test_stock_initialize ;;
            22) test_stock_add ;;
            23) test_stock_remove ;;
            24) test_stock_reserve ;;
            25) test_stock_get ;;
            26) test_stock_get_all ;;
            27) test_stock_low_stock ;;
            28) test_order_place ;;
            29) test_order_get ;;
            30) test_order_get_pending ;;
            31) test_order_get_all ;;
            32) test_order_update_status ;;
            33) test_order_receive ;;
            99) run_complete_workflow ;;
            0) echo "Exiting..."; exit 0 ;;
            *) echo "Invalid choice!" ;;
        esac

        read -p "Press Enter to continue..."
    done
else
    # Command mode
    case $1 in
        all) run_complete_workflow ;;
        auth) test_auth_register_mechanic; test_auth_login_mechanic ;;
        vehicle) test_vehicle_create ;;
        service) test_service_create ;;
        product) test_product_create ;;
        stock) test_stock_initialize ;;
        order) test_order_place ;;
        *) echo "Unknown command: $1"; exit 1 ;;
    esac
fi

