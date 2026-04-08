# Castorama ATG Commerce

A Spring Boot 3 application modelling Oracle ATG Commerce patterns for a French DIY/home improvement retail store (Castorama France). Demonstrates Nucleus-style IoC components, GSA-style repositories, pipeline-based checkout, JWT-secured REST APIs, and a fully functional SPA frontend.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA |
| Auth | JWT (JJWT 0.12) + BCrypt |
| Database | H2 (file-based, persists across restarts) |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Frontend | Vanilla HTML/CSS/JS SPA (no framework) |
| Build | Maven 3 |

---

## ATG Patterns Implemented

| ATG Concept | Implementation |
|---|---|
| Nucleus IoC container | Spring `@Service` / `@Repository` singleton beans |
| GSA Repository | Spring Data JPA with JPQL (equivalent to RQL) |
| Item Descriptors | JPA `@Entity` models (`User`, `Product`, `CartItem`, `Order`) |
| Pipeline chain | `CheckoutPipeline` — 6 ordered `PipelineProcessor` beans |
| Pipeline context | `PipelineContext` typed POJO (replaces ATG's raw `HashMap`) |
| ProfileFormHandler | `UserService.register()` + `AuthController` |
| LoginFormHandler | `UserService.login()` + JWT issuance |
| CartModifierFormHandler | `CartService` + `CartController` |
| OrderManager | `OrderService` + checkout pipeline |
| ATG Price Engine | `PriceCalculationProcessor` (TVA 20%, free shipping ≥ €75) |
| Dynamo Administration UI | `DynAdminController` — server-rendered Dyn/Admin at `/dyn/admin` |

---

## Project Structure

```
src/main/java/com/castorama/atg/
├── admin/                # DynAdminController, DynAdminProperties — Dyn/Admin UI
├── config/               # SecurityConfig, WebConfig
├── controller/           # AuthController, CatalogController, CartController, OrderController, SpaController
├── domain/
│   ├── enums/            # ProductCategory, OrderStatus
│   └── model/            # User, Product, CartItem, OrderItem, Order
├── dto/
│   ├── request/          # RegisterRequest, LoginRequest, CartItemRequest, CheckoutRequest
│   └── response/         # AuthResponse, ProductResponse, CartResponse, OrderResponse, ...
├── exception/            # GlobalExceptionHandler (RFC 7807), BusinessException, ...
├── init/                 # CatalogDataInitializer — 21 French products seeded at startup
├── pipeline/             # CheckoutPipeline, PipelineContext, PipelineProcessor
│   └── processor/        # Validate → Inventory → Price → Payment → Reserve → Finalise
├── repository/           # UserRepository, ProductRepository, CartItemRepository, OrderRepository
├── security/             # JwtTokenService, JwtAuthenticationFilter, NucleusUserDetailsService
└── service/              # UserService, CatalogService, CartService, OrderService

src/main/resources/
├── static/
│   ├── index.html        # SPA shell
│   ├── css/castorama.css # Castorama-style UI
│   └── js/
│       ├── api.js        # REST client (fetch wrapper)
│       └── app.js        # SPA routing, rendering, state
└── application.properties
```

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+

### Run

```bash
git clone https://github.com/<your-username>/castorama-atg.git
cd castorama-atg
mvn spring-boot:run
```

The database file is created at `./data/castorama_atg.mv.db` on first run and persists across restarts.

### Access

| URL | Description |
|---|---|
| `http://localhost:8080` | SPA Website |
| `http://localhost:8080/swagger-ui.html` | Swagger UI (try all endpoints) |
| `http://localhost:8080/dyn/admin` | Dyn/Admin interface (HTTP Basic Auth) |
| `http://localhost:8080/h2-console` | H2 Database console |

H2 console JDBC URL: `jdbc:h2:file:./data/castorama_atg`

Default Dyn/Admin credentials: `admin` / `admin123` (configurable via `castorama.admin.username` / `castorama.admin.password`).

---

## API Endpoints

### Auth
| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | — | Register new account, returns JWT |
| `POST` | `/api/v1/auth/login` | — | Login, returns JWT |
| `GET` | `/api/v1/auth/me` | Bearer | Current user profile |

### Catalog
| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/catalog/products` | — | All products (paginated) |
| `GET` | `/api/v1/catalog/products/{id}` | — | Product by ID |
| `GET` | `/api/v1/catalog/products/sku/{sku}` | — | Product by SKU |
| `GET` | `/api/v1/catalog/categories/{cat}/products` | — | Products by category |
| `GET` | `/api/v1/catalog/search?q={query}` | — | Keyword search |
| `GET` | `/api/v1/catalog/products/on-sale` | — | Promotional items |

### Cart
| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/cart` | Bearer | View cart |
| `POST` | `/api/v1/cart/items` | Bearer | Add item `{ skuCode, quantity }` |
| `PUT` | `/api/v1/cart/items/{id}?quantity=N` | Bearer | Update quantity |
| `DELETE` | `/api/v1/cart/items/{id}` | Bearer | Remove item |
| `DELETE` | `/api/v1/cart` | Bearer | Clear cart |

### Orders
| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/orders/checkout` | Bearer | Checkout — runs 6-step pipeline |
| `GET` | `/api/v1/orders` | Bearer | Order history (paginated) |
| `GET` | `/api/v1/orders/{orderNumber}` | Bearer | Order detail |
| `POST` | `/api/v1/orders/{orderNumber}/cancel` | Bearer | Cancel order |

---

## Checkout Pipeline

The checkout flow mirrors ATG's `PipelineManager.runProcess()`:

```
[1] ValidateCartProcessor      — cart not empty, quantities valid
[2] InventoryCheckProcessor    — sufficient stock for all items
[3] PriceCalculationProcessor  — subtotal, TVA 20%, shipping (free ≥ €75)
[4] PaymentAuthProcessor       — payment method validation
[5] ReserveInventoryProcessor  — decrement stock
[6] FinaliseOrderProcessor     — persist order, status → CONFIRMED
```

Any processor can halt the chain by calling `ctx.addError(message)`, which sets `stopChain = true` (equivalent to ATG's `STOP_CHAIN_EXECUTION`).

---

## Product Catalogue

21 products across 10 categories seeded at startup:

| Category | French | Examples |
|---|---|---|
| `OUTILLAGE` | Outillage | Bosch GSR 18V drill, DeWalt grinder, Leica laser level |
| `PEINTURE` | Peinture | Dulux Valentine 10L, Tollens façade paint |
| `SOL` | Sols & Murs | Porcelain tiles 60×60, Quick-Step laminate flooring |
| `PLOMBERIE` | Plomberie | Grohe mixer tap, Thermor water heater 150L |
| `ELECTRICITE` | Électricité | Philips LED pack ×10, Legrand distribution board |
| `JARDIN` | Jardin | Bosch Rotak lawnmower, Compo compost 50L |
| `MENUISERIE` | Menuiserie | Interior door 204×73cm |
| `QUINCAILLERIE` | Quincaillerie | Fischer wall plugs ×100, Vachette 3-point lock |
| `CHAUFFAGE` | Chauffage | Atlantic Galéa 1500W electric heater |
| `CUISINE` | Cuisine & Bain | Franke stainless steel sink |

---

## Registration

```json
POST /api/v1/auth/register
{
  "login": "jean.dupont",
  "firstName": "Jean",
  "lastName": "Dupont",
  "email": "jean@example.fr",
  "password": "motdepasse123"
}
```

**Constraints:** `login` min 3 chars, `password` min 8 chars, valid email format.

The website derives `login` automatically from the email local-part, so users only fill in first name, last name, email, and password.

---

## Dyn/Admin Interface

A server-rendered administration UI modelled after Oracle ATG's built-in `/dyn/admin` console, protected by HTTP Basic Auth.

| Page | Path | Description |
|---|---|---|
| Dashboard | `/dyn/admin` | Component counts, JVM memory, uptime |
| Nucleus Browser | `/dyn/admin/nucleus` | All Spring beans (Nucleus component tree) |
| User Repository | `/dyn/admin/repo/users` | Browse and delete user accounts |
| Product Repository | `/dyn/admin/repo/products` | Paginated product catalogue |
| Order Repository | `/dyn/admin/repo/orders` | All orders with status and totals |
| Pipeline Viewer | `/dyn/admin/pipeline` | Checkout pipeline processor chain |
| System Properties | `/dyn/admin/system` | JVM system properties |

---

## Configuration

Key properties in `application.properties`:

```properties
# File-based H2 (persists across restarts)
spring.datasource.url=jdbc:h2:file:./data/castorama_atg;DB_CLOSE_DELAY=-1;MODE=Oracle

# JWT
castorama.jwt.secret=<min-32-char-secret>
castorama.jwt.expiration-seconds=86400

# Schema
spring.jpa.hibernate.ddl-auto=update

# Dyn/Admin credentials
castorama.admin.username=admin
castorama.admin.password=admin123
```

---

## License

MIT
