#  UrbanVogue Apparel — E-Commerce Microservices Platform




> **A production-grade, enterprise-level e-commerce backend built entirely with Spring Boot Microservices Architecture.**
> Powering the online marketplace for UrbanVogue Apparel — scalable, secure, and engineered to handle End-of-Season sale traffic.

<br/>

|  Microservices |  REST APIs |  DB Tables |  Integrations |  Notification Types |
|:-:|:-:|:-:|:-:|:-:|
| **9** | **54** | **9** | **12** | **9** |

</div>

---

## 1. Architecture Overview

UrbanVogue is built on a **9-service microservices architecture** where every service is independently deployable, owns a single business domain, and communicates via REST. No client ever interacts with a downstream service directly — all traffic is funnelled through a single **API Gateway** that handles authentication, extracts identity, and routes the request appropriately.

The platform was designed to meet three core non-functional requirements set by UrbanVogue's CTO:

- **Scalability** — during End-of-Season sales, only the Order and Payment services need to scale; others remain untouched.
- **Security** — JWT tokens are validated at the edge (Gateway), BCrypt protects all passwords, and Role-Based Access Control enforces admin-only catalog operations.
- **Maintainability** — each service is a clean, focused Spring Boot application with its own controller, service, repository, and model layer.

<br/>
<br/>

### Key Architectural Decisions

| Decision | Why It Was Made |
|---|---|
| **Microservices over Monolith** | Each service scales independently — Order and Payment can scale during flash sales without touching the catalog |
| **API Gateway as Edge** | Single entry point keeps internal ports hidden and centralises cross-cutting concerns like JWT validation |
| **Shared H2 File Database** | Development simplicity — all 9 services connect to one `UrbanVogueDB.mv.db` file enabling real-time cross-service data visibility |
| **Stateless JWT Authentication** | No server-side sessions — the token carries identity and role; the gateway validates it locally without a network call to Auth Service |
| **RestTemplate for IPC** | Synchronous REST calls between services — simple, predictable, easy to debug in a development environment |
| **Graceful Degradation** | All non-critical calls (notifications) are wrapped in try-catch — a notification failure never blocks an order or payment |
| **Price Snapshot at Order Time** | `priceAtPurchase` is frozen when an order is placed — admin discount changes never corrupt historical receipts |

---

## 2. Technology Stack

### Backend Framework

| Technology | Version | Role in Project |
|---|---|---|
| Java | 17 LTS | Primary programming language across all 9 services |
| Spring Boot | 3.2.0 | Core application framework — auto-configuration, embedded Tomcat |
| Spring Web MVC | 6.1.1 | REST API layer — controllers, request mapping, response serialisation |
| Spring Data JPA | 3.2.0 | Database ORM layer — repository pattern, derived queries |
| Spring Security | 6.2.0 | Security framework — filter chain, BCrypt, HTTP security config |
| Spring Cloud Gateway | 2023.0.0 | API Gateway — reactive routing, global filter, route predicates |
| Hibernate ORM | 6.3.1 | JPA implementation — entity management, DDL generation |

### Security

| Technology | Version | Role in Project |
|---|---|---|
| JJWT API | 0.11.5 | JWT token generation interface |
| JJWT Impl | 0.11.5 | JWT implementation — signing, parsing, validation |
| JJWT Jackson | 0.11.5 | JWT JSON serialisation via Jackson |
| BCryptPasswordEncoder | Spring Security | One-way password hashing — 60-character `$2a$10$...` output |

### Database & Persistence

| Technology | Version | Role in Project |
|---|---|---|
| H2 Database | 2.2.224 | Embedded file-based database shared across all 9 services |
| HikariCP | 5.0.1 | Database connection pool — manages connections per service |
| JPA 3.1 | Jakarta | Persistence API standard — `@Entity`, `@OneToMany`, `@ManyToOne` |

### Development & Build

| Tool | Role in Project |
|---|---|
| Maven 3.x | Multi-module build tool — parent POM manages all 9 child modules |
| Lombok 1.18.30 | Code generation — `@Data`, `@Getter`, `@Setter`, `@NoArgsConstructor` |
| IntelliJ IDEA CE | Primary IDE — run configurations for all 9 services simultaneously |
| Postman | API testing — collections, environment variables, token management |
| Git | Version control — `.gitignore` excludes IDE files, target/, database files |
| Apache Tomcat 10.1 | Embedded servlet container in every Spring Boot service |

---

## 3. Microservices

The platform is composed of **9 microservices**. The table below provides a quick orientation before each section dives into the detail.

| # | Service | Port | Primary Responsibility | Calls Out To |
|---|---|---|---|---|
| 1 | API Gateway | 8080 | JWT validation, routing | All 8 services |
| 2 | Auth Service | 8082 | Login, register, JWT issuance | User, Notification |
| 3 | Product Service | 8091 | Catalog management, stock control | Nobody |
| 4 | Order Service | 8083 | Order lifecycle, stock deduction | Product, Notification |
| 5 | Payment Service | 8084 | Transactions, refunds, cart clearing | Order, Cart, Notification |
| 6 | Notification Service | 8085 | Email & SMS for all events | Nobody |
| 7 | Admin Service | 8086 | Bulk operations, analytics | Nobody (shared DB) |
| 8 | User Service | 8087 | Profiles, dashboard aggregation | Order, Auth |
| 9 | Cart Service | 8089 | Shopping cart persistence | Nobody |

---

### 3.1 API Gateway — Port 8080

The API Gateway is the **single entry point** for every request in the UrbanVogue platform. No client ever calls a downstream service port directly — they only ever speak to port 8080.

#### What It Does

The gateway performs two jobs on every incoming request. First, it runs the `AuthenticationFilter`, which determines whether the request carries a valid JWT token. For public paths (registration, login, product browsing), it lets the request through immediately. For protected paths, it extracts and validates the JWT, then injects the verified username as the `X-Logged-In-User` header before forwarding. Second, it matches the request path to the correct downstream service using the route table in `application.yml`.

#### Public vs Protected Paths
|PUBLIC  (no token required)     |     PROTECTED  (JWT required)     |
|--|--|
|/api/auth/register           |        /api/cart/** |
|/api/auth/login           |           /api/orders/**|
|/api/products/**           |          /api/payments/**|
|/api/users/exists/**        |         /api/users/me|
|/api/users/me/dashboard |

---

### 3.2 Auth Service — Port 8082

The Auth Service is the **security backbone** of UrbanVogue. It is the only service that knows passwords and the only service that issues JWT tokens.

#### What It Does

When a user registers, the Auth Service encrypts the password using BCrypt and saves the credentials. It then calls the User Service to create the rich profile and calls the Notification Service to send the welcome email — all in a single registration request. When a user logs in, the service retrieves the stored BCrypt hash, verifies the raw password against it using `BCryptPasswordEncoder.matches()`, and if valid, generates a signed JWT containing the username and role with a 72-hour expiry.

#### Key Design Decisions

- `BCryptPasswordEncoder` is declared as a `@Bean` in `SecurityConfig` so Spring manages a single instance shared between `AuthController` and `AuthService`. Password encoding happens **once only** in `AuthController` before saving — `AuthService.register()` only calls `userRepository.save()`.
- The `RegistrationRequest` DTO captures both authentication fields (`username`, `password`, `role`) and profile fields (`fullName`, `age`, `sex`, `address`, `email`) enabling Amazon-style single-form registration.
- `RestTemplate` is declared as a `@Bean` in `AuthServiceApplication` to support the two outbound calls (User Service and Notification Service) made during registration.

**Database Table:** `USERS (id, username, password, role)`

---

### 3.3 Product Service — Port 8091

The Product Service is the **catalog management core** — the digital storefront shelf of UrbanVogue.

#### What It Does

The service manages the complete product lifecycle. GET endpoints are entirely public — any visitor can browse the catalog without authentication. Write operations (create, update, delete) are admin-only, enforced by checking the `X-User-Role` request header. Two internal endpoints handle stock mutations: `reduce-stock` is called by the Order Service when an order is placed, and `restore-stock` is called when an order is cancelled.

#### Business Rules Applied in `ProductService`
addProduct(): <br>
Rule 1 → price must be > 0 
throws "Price must be a positive value" <br>
Rule 2 → stockQuantity must be >= 0 
throws "Initial stock cannot be negative" <br>
Rule 3 → name.toUpperCase() applied before saving
"Urban Classic Sneakers" → "URBAN CLASSIC SNEAKERS" <br>
reduceStock(): <br>
Rule 1 → available stock must be >= requested quantity
throws "Insufficient stock for product: X" <br>
Rule 2 → newStock = current - ordered; saved immediately

**Database Table:** `PRODUCTS (id, name, description, price, stock_quantity, category)`

---

### 3.4 Order Service — Port 8083

The Order Service is the **transaction backbone** — the digital billing counter that sits between a customer's intent to buy and the payment process.

#### What It Does

The service manages the complete order lifecycle from placement through delivery. It validates every item in the order against the Product Service before saving anything, auto-calculates the order total (clients cannot send a manipulated total), reduces stock after a successful save, and triggers notifications at key lifecycle stages.

#### Order Placement — 6-Step Flow

POST /api/orders  (with Authorization: Bearer <token>) <br> <br>

  Gateway injects X-Logged-In-User: john_doe <br>
  OrderController: request.setCustomerUsername("john_doe") <br><br>

**STEP 1** → Validate username not blank  <br>
Validate items list not empty <br><br>

**STEP 2** → For each item:   <br>
• quantity must be > 0  <br>
• priceAtPurchase must be > 0 <br>
• GET /api/products/get/{id} → must return 200 <br><br>

**STEP 3** → Convert DTOs to OrderItem entities <br><br>

**STEP 4** → Calculate total = Σ(priceAtPurchase × quantity) <br>
Round to 2 decimal places <br><br>

**STEP 5** → Build Order entity <br>
status = "PENDING" <br>
Link all OrderItems to the Order <br>
Save (CascadeType.ALL saves items too) <br><br>

**STEP 6** → For each item: <br>
PUT /api/products/{id}/reduce-stock?quantity=X <br>
If any fail → mark order CANCELLED → throw error <br> <br>


POST /api/notifications/order-placed <br>
(wrapped in try-catch — failure won't cancel order) <br>


Return saved Order with items<br>

> `DELIVERED` and `CANCELLED` are terminal states — no further status changes are accepted.

**Database Tables:** `ORDERS`, `ORDER_ITEMS`

---

### 3.5 Payment Service — Port 8084

The Payment Service is the **most security-critical service** in the platform — it handles all financial transactions and triggers the cart clearing that closes the checkout loop.

#### What It Does

The service accepts a payment request, validates it against four business rules, generates a unique transaction ID, saves a `PENDING` record before touching the gateway (ensuring audit trail integrity even if the server crashes mid-transaction), simulates the payment gateway, and then updates the order and clears the cart on success.
#### Payment Flow

POST /api/payments/initiate  
│  
▼  
Business Rule Checks:  
✦ Amount must be > 0  
✦ Payment method must be provided  
✦ Method must be UPI / CARD / NETBANKING / COD  
✦ No existing payment for this orderId  
│  
▼  
Generate transactionId → "UV-TXN-" + UUID(0..8).toUpperCase()  
│  
▼  
Save Payment with status = PENDING   ← Audit trail preserved here  
│  
▼  
simulateGateway(paymentMethod):  
COD   → always true  
Other → Math.random() < 0.8  (80% success)  
│  
┌────┴────┐  
SUCCESS     FAILED   
status = SUCCESS   status = FAILED   failure_reason =  "Payment declined"  
│  
├──► PUT /api/orders/{id}/status?newStatus=CONFIRMED  
├──► DELETE /api/cart/clear  (with X-Logged-In-User header)  
└──► POST /api/notifications/payment-success
``

#### Refund Flow

POST /api/payments/refund/{transactionId}  
│  
▼  
Find payment by transactionId  
│  
▼  
Status must be SUCCESS  
(FAILED / PENDING / REFUNDED → error)  
│  
▼  
Update status → REFUNDED  
│  
├──► PUT /api/orders/{id}/status?newStatus=CANCELLED  
└──► POST /api/notifications/refund-initiated
``

**Database Table:** `PAYMENTS (id, order_id, customer_username, amount, payment_method, status, transaction_id, failure_reason, created_at, updated_at)`

---

### 3.6 Notification Service — Port 8085

The Notification Service is the **communication layer** — a dedicated service whose sole job is sending emails and SMS messages at every significant business event.

#### What It Does

The service exposes 9 predefined event endpoints (called automatically by Auth, Order, and Payment services) and 1 generic send endpoint for custom messages. Each notification is saved to the database before being "sent" — in development, sending is simulated by printing a formatted box to the IntelliJ console. In production, `simulateSending()` would be replaced with a JavaMailSender call for EMAIL and a Twilio REST call for SMS.

#### Notification Type → Channel → Trigger Matrix

| Event            | Channel | Triggered By     | When                    |
|-----------------|--------|------------------|-------------------------|
| WELCOME         | EMAIL  | Auth Service     | On registration         |
| LOGIN           | EMAIL  | Auth Service     | On every login          |
| ORDER_PLACED    | EMAIL  | Order Service    | Order saved             |
| ORDER_SHIPPED   | EMAIL  | Order Service    | Status → SHIPPED        |
| ORDER_DELIVERED | EMAIL  | Order Service    | Status → DELIVERED      |
| ORDER_CANCELLED | EMAIL  | Order Service    | Order cancelled         |
| PAYMENT_SUCCESS | SMS    | Payment Service  | Payment SUCCESS         |
| PAYMENT_FAILED  | SMS    | Payment Service  | Payment FAILED          |
| REFUND_INITIATED| SMS    | Payment Service  | Refund processed        |

> **Design Principle:** Financial events (payment, refund) use SMS because they are urgent and time-sensitive. Informational events (order placed, shipped) use EMAIL for richer formatting. All calls from other services are wrapped in try-catch — if this service is down, no order or payment is blocked.

**Database Table:** `NOTIFICATIONS (id, recipient_username, type, message, channel, status, reference_id, created_at, sent_at)`

---

### 3.7 Admin Service — Port 8086

The Admin Service is the **business intelligence command center** — it gives UrbanVogue management operational visibility and the ability to execute bulk operations without touching customer-facing services.

#### What It Does

The service provides dashboard statistics (total products, out-of-stock count, total inventory value), applies percentage discounts to entire product categories in one call, restocks categories after warehouse shipments, and can remove discontinued products. Because it shares the same H2 database file as the Product Service, all changes made through Admin Service are **immediately visible** to customers browsing through the Product Service.

#### Why It's Separate From Product Service

If an admin runs a calculation across all 10,000 products to generate a financial report, that query takes several seconds and consumes significant CPU. If that logic lived inside the Product Service, every customer browsing the catalog would experience slowness during the calculation. By isolating admin operations in a separate service on a separate port, heavy analytics never compete with customer-facing traffic.

**Shared Table:** `PRODUCTS` — Admin Service owns its own `Product` entity and `ProductRepository` mapped to the same table used by Product Service.

---

### 3.8 User Service — Port 8087

The User Service is the **customer profile manager** — it stores rich demographic data and provides an Amazon-style integrated dashboard that aggregates profile and order history into a single response.

#### What It Does

When Auth Service completes registration, it calls User Service to create the profile record containing fullName, age, sex, address, and email. The `/me` endpoints read the `X-Logged-In-User` header injected by the Gateway — the username never comes from the URL, making it impossible for one user to read another's profile. The dashboard endpoint calls the Order Service internally and merges the response with the profile before returning.

#### Dashboard Aggregation

1.GET /api/users/me/dashboard (X-Logged-In-User: john_doe from Gateway) <br>
2.getProfile("john_doe")  →  USER_PROFILES table <br>
3.GET /api/orders/customer/john_doe  →  Order Service <br>
4.UserProfileDashboard {<br>
profile:      { username, fullName, age, address, email } <br>
orderHistory: [ { id, status, totalAmount, items... } ] <br>
} <br>
5.Single JSON response returned to client

#### Cascade Account Deletion
1.DELETE /api/users/{username}<br>
2.Delete from USER_PROFILES table<br>
3.DELETE /api/auth/{username}  →  Auth Service <br>
4.Delete from USERS table <br>
"Account successfully deleted for user: X"

**Database Table:** `USER_PROFILES (id, username, full_name, age, sex, address, email)`

---

### 3.9 Cart Service — Port 8089

The Cart Service is the **shopping cart persistence layer** — it maintains each customer's cart state between browsing and checkout.

#### What It Does

The service provides three endpoints: view cart, add item, and clear cart. All three read the username exclusively from the `X-Logged-In-User` header — the username is never accepted in the request body, making it impossible for a malicious client to manipulate another user's cart. If no cart exists for a user when they first add an item, one is created lazily. When the same product is added twice, quantities are merged intelligently rather than creating a duplicate row. After a successful payment, the Payment Service calls `DELETE /api/cart/clear` passing the `X-Logged-In-User` header so the cart is automatically emptied.

#### Smart Item Merge Logic
POST /api/cart/add  { productId: 1, quantity: 2 }
Cart exists for user?
├── NO  →  Create new Cart record  →  Add item
└── YES →  Does productId: 1 already exist in cart?
├── NO  →  Add as new CartItem
└── YES →  existingItem.quantity += newItem.quantity
(2 + 1 = 3, not two separate rows)

**Database Tables:** `CART (id, username)` · `CART_ITEMS (id, cart_id, product_id, product_name, price, quantity)`

---
## 4. Authentication & Authorization

### How JWT Works in This System

UrbanVogue uses **stateless JWT authentication**. The server never stores session data. Instead, the token itself carries the identity — every service that needs to know who the user is reads the `X-Logged-In-User` header that the Gateway injects after verifying the token.

---

## 5. API Reference

> **Base URL:** `http://localhost:8080` (API Gateway)
>
> **Authentication:** Protected endpoints require `Authorization: Bearer <token>` header.
> Get your token from `POST /api/auth/login`.

---

### Auth Service — 4 APIs

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | ❌ None | Register user — creates credentials + profile + sends welcome email |
| `POST` | `/api/auth/login` | ❌ None | Login — returns JWT token + user details |
| `POST` | `/api/auth/validate` | ✅ Bearer | Validates if JWT is still active and not expired |
| `DELETE` | `/api/auth/{username}` | ✅ Bearer | Deletes login credentials (called by User Service on account deletion) |

**Register — Request Body:**
```json
{
    "username":  "john_doe",
    "password":  "password123",
    "role":      "ROLE_CUSTOMER",
    "fullName":  "John Doe",
    "age":       28,
    "sex":       "Male",
    "address":   "123 MG Road, Bangalore",
    "email":     "john@gmail.com"
}
```

**Login — Response:**
```json
{
    "success":  true,
    "token":    "eyJhbGciOiJIUzI1NiJ9...",
    "username": "john_doe",
    "role":     "ROLE_CUSTOMER",
    "userId":   1,
    "message":  "Login successful!"
}
```

---

### User Service — 7 APIs

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/users` | ✅ Bearer | Create or update user profile (upsert) |
| `GET` | `/api/users/me` | ✅ Bearer | Get own profile — username from JWT |
| `GET` | `/api/users/me/dashboard` | ✅ Bearer | Profile + full order history in one response |
| `GET` | `/api/users/exists/{username}` | ❌ None | Check if profile exists — returns `true`/`false` |
| `GET` | `/api/users/{username}` | ✅ Bearer | Get profile by username (internal use) |
| `GET` | `/api/users/{username}/dashboard` | ✅ Bearer | Dashboard by username (internal use) |
| `DELETE` | `/api/users/{username}` | ✅ Bearer | Delete account — cascades to Auth Service |

---

### Product Service — 7 APIs

| Method | Endpoint | Auth Required | Extra Header | Description |
|---|---|---|---|---|
| `GET` | `/api/products` | ❌ None | — | Browse full product catalog |
| `GET` | `/api/products/get/{id}` | ❌ None | — | Get single product details |
| `POST` | `/api/products` | ✅ Bearer | `X-User-Role: ROLE_ADMIN` | Create new product |
| `PUT` | `/api/products/{id}` | ✅ Bearer | `X-User-Role: ROLE_ADMIN` | Update product details or price |
| `DELETE` | `/api/products/{id}` | ✅ Bearer | `X-User-Role: ROLE_ADMIN` | Remove product from catalog |
| `PUT` | `/api/products/{id}/reduce-stock?quantity=X` | ❌ None | — | Reduce stock — called by Order Service |
| `PUT` | `/api/products/{id}/restore-stock?quantity=X` | ❌ None | — | Restore stock — called on cancellation |

**Create Product — Request Body:**
```json
{
    "name":          "Urban Classic Sneakers",
    "description":   "Premium white leather sneakers",
    "price":         2499.99,
    "stockQuantity": 50,
    "category":      "Footwear"
}
```

---

### Cart Service — 3 APIs

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `GET` | `/api/cart` | ✅ Bearer | View current cart — username from JWT |
| `POST` | `/api/cart/add` | ✅ Bearer | Add item to cart — merges if product already exists |
| `DELETE` | `/api/cart/clear` | ✅ Bearer | Clear entire cart — called by Payment Service after payment |

**Add to Cart — Request Body:**
```json
{
    "productId":   1,
    "productName": "URBAN CLASSIC SNEAKERS",
    "price":       2499.99,
    "quantity":    2
}
```

> ⚠️ **Note:** Username is never sent in the body. It is automatically read from the `X-Logged-In-User` header injected by the Gateway.

---

### Order Service — 6 APIs

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/orders` | ✅ Bearer | Place order — username auto-injected from JWT |
| `GET` | `/api/orders` | ✅ Bearer | Get all orders (admin view) |
| `GET` | `/api/orders/{id}` | ✅ Bearer | Get single order with all items |
| `GET` | `/api/orders/customer/{username}` | ✅ Bearer | Get customer's order history |
| `PUT` | `/api/orders/{id}/status?newStatus=X` | ✅ Bearer | Update order status |
| `DELETE` | `/api/orders/{id}` | ✅ Bearer | Cancel order — PENDING only; restores stock |

**Place Order — Request Body:**
```json
{
    "items": [
        {
            "productId":       1,
            "productName":     "URBAN CLASSIC SNEAKERS",
            "quantity":        2,
            "priceAtPurchase": 2499.99
        },
        {
            "productId":       2,
            "productName":     "SLIM FIT JACKET",
            "quantity":        1,
            "priceAtPurchase": 3999.00
        }
    ]
}
```

> ⚠️ `customerUsername` is **not** required in the body. The Gateway injects `X-Logged-In-User` and the controller uses it automatically.

---

### Payment Service — 7 APIs

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/payments/initiate` | ✅ Bearer | Process payment for an order |
| `POST` | `/api/payments/refund/{transactionId}` | ✅ Bearer | Refund a SUCCESS payment |
| `GET` | `/api/payments/order/{orderId}` | ✅ Bearer | Find payment by order ID |
| `GET` | `/api/payments/transaction/{transactionId}` | ✅ Bearer | Find payment by transaction ID |
| `GET` | `/api/payments/customer/{username}` | ✅ Bearer | Customer's payment history |
| `GET` | `/api/payments` | ✅ Bearer | All payments — admin view |
| `GET` | `/api/payments/status/{status}` | ✅ Bearer | Filter: SUCCESS / FAILED / REFUNDED |

**Initiate Payment — Request Body:**
```json
{
    "orderId":           3,
    "customerUsername":  "john_doe",
    "amount":            4999.98,
    "paymentMethod":     "UPI"
}
```

**Payment Success — Response:**
```json
{
    "paymentId":      1,
    "orderId":        3,
    "transactionId":  "UV-TXN-A1B2C3D4",
    "status":         "SUCCESS",
    "amount":         4999.98,
    "paymentMethod":  "UPI",
    "message":        "Payment successful! Your order has been confirmed. Transaction ID: UV-TXN-A1B2C3D4"
}
```

---

### Notification Service — 14 APIs

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/notifications/send` | ✅ Bearer | Send a fully custom notification |
| `POST` | `/api/notifications/welcome?username=X` | ✅ Bearer | Send welcome email on registration |
| `POST` | `/api/notifications/order-placed?username=X&orderId=X&amount=X` | ✅ Bearer | Order placed confirmation email |
| `POST` | `/api/notifications/payment-success?username=X&orderId=X&amount=X&transactionId=X` | ✅ Bearer | Payment success SMS |
| `POST` | `/api/notifications/payment-failed?username=X&orderId=X&amount=X` | ✅ Bearer | Payment failed SMS |
| `POST` | `/api/notifications/order-shipped?username=X&orderId=X` | ✅ Bearer | Order shipped email |
| `POST` | `/api/notifications/order-delivered?username=X&orderId=X` | ✅ Bearer | Order delivered email |
| `POST` | `/api/notifications/order-cancelled?username=X&orderId=X` | ✅ Bearer | Order cancelled email |
| `POST` | `/api/notifications/refund-initiated?username=X&orderId=X&amount=X` | ✅ Bearer | Refund initiated SMS |
| `GET` | `/api/notifications` | ✅ Bearer | All notifications — admin view |
| `GET` | `/api/notifications/{id}` | ✅ Bearer | Get single notification |
| `GET` | `/api/notifications/user/{username}` | ✅ Bearer | User's notification inbox |
| `GET` | `/api/notifications/type/{type}` | ✅ Bearer | Filter by event type |
| `GET` | `/api/notifications/status/{status}` | ✅ Bearer | Filter by delivery status |

---

### Admin Service — 6 APIs

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `GET` | `/api/admin/stats` | ✅ Bearer | Dashboard — total products, out-of-stock, inventory value |
| `GET` | `/api/admin/stats/inventory-value` | ✅ Bearer | Total financial value of all stock |
| `GET` | `/api/admin/products` | ✅ Bearer | Admin catalog view (shared DB — real-time data) |
| `PUT` | `/api/admin/products/bulk-discount?category=X&percent=Y` | ✅ Bearer | Apply % discount to entire category |
| `PUT` | `/api/admin/products/restock?category=X&quantity=Y` | ✅ Bearer | Add quantity to all products in category |
| `DELETE` | `/api/admin/products/{id}` | ✅ Bearer | Remove discontinued product |

---

