# API Guidelines

## Purpose

This document defines the REST API conventions for the CYNA Platform. All backend endpoints must follow these guidelines to ensure consistency, predictability, and maintainability.

---

## Base URL

```
https://api.cyna.com/api/v{version}/{resource}
```

| Environment | Base URL |
|-------------|----------|
| Local | `http://localhost:8080/api/v1` |
| Staging | `https://api-staging.cyna.com/api/v1` |
| Production | `https://api.cyna.com/api/v1` |

---

## Versioning

API versioning is done via the **URL path**:

```
/api/v1/products
/api/v2/products    (future breaking change)
```

**Rules:**
- Major version in the URL path: `/api/v1/...`
- Minor/backward-compatible changes do not require a new version.
- Breaking changes require a new version and a deprecation period for the old one.
- Never more than two active versions at the same time.

---

## Resource Naming

### Conventions

| Rule | Example |
|------|---------|
| Plural nouns for collections | `/api/v1/products`, not `/api/v1/product` |
| Lowercase, hyphen-separated | `/api/v1/order-lines`, not `/api/v1/orderLines` |
| No verbs in URLs | `/api/v1/orders` + `POST`, not `/api/v1/createOrder` |
| Nesting for ownership | `/api/v1/orders/{orderId}/lines` |
| Maximum 2 levels of nesting | `/api/v1/orders/{orderId}/lines`, not `/api/v1/customers/{id}/orders/{id}/lines/{id}/details` |
| UUIDs for resource identifiers | `/api/v1/products/550e8400-e29b-41d4-a716-446655440000` |

### Resource Examples

```
GET    /api/v1/products                    → List products
GET    /api/v1/products/{id}               → Get product by ID
POST   /api/v1/products                    → Create product
PUT    /api/v1/products/{id}               → Full update product
PATCH  /api/v1/products/{id}               → Partial update product
DELETE /api/v1/products/{id}               → Delete product

GET    /api/v1/orders/{orderId}/lines      → List order lines
POST   /api/v1/orders/{orderId}/lines      → Add order line
```

### File Upload Pattern

Binary resources (images, documents) are **never embedded in the main resource JSON body**. They are managed via a dedicated sub-resource endpoint using `multipart/form-data`:

```
PATCH  /api/v1/categories/{id}/image       → Upload or replace category image
PATCH  /api/v1/products/{id}/image         → Upload or replace product image
```

**Rationale:**
- Keeps CRUD endpoints as `application/json` — consistent and easy to test
- Decouples metadata updates from binary uploads (frontend can do them independently)
- Image upload response is `204 No Content` (no body needed)
- Image data is returned as Base64 in GET responses (`imageBase64` field, nullable)

---

## HTTP Methods

| Method | Purpose | Idempotent | Request Body | Response |
|--------|---------|------------|-------------|----------|
| `GET` | Read resource(s) | Yes | No | Resource or collection |
| `POST` | Create resource | No | Yes | Created resource + `201` |
| `PUT` | Full replacement | Yes | Yes | Updated resource + `200` |
| `PATCH` | Partial update | No* | Yes | Updated resource + `200` |
| `DELETE` | Remove resource | Yes | No | `204 No Content` |

---

## HTTP Status Codes

### Success

| Code | Meaning | When |
|------|---------|------|
| `200 OK` | Request succeeded | GET, PUT, PATCH |
| `201 Created` | Resource created | POST |
| `204 No Content` | Success, no body | DELETE |

### Client Errors

| Code | Meaning | When |
|------|---------|------|
| `400 Bad Request` | Invalid input | Validation failure, malformed JSON |
| `401 Unauthorized` | Not authenticated | Missing or invalid JWT |
| `403 Forbidden` | Not authorized | Insufficient role/permissions |
| `404 Not Found` | Resource doesn't exist | GET/PUT/PATCH/DELETE on unknown ID |
| `409 Conflict` | Business conflict | Duplicate email, invalid state transition |
| `422 Unprocessable Entity` | Business rule violation | Domain validation failure |

### Server Errors

| Code | Meaning | When |
|------|---------|------|
| `500 Internal Server Error` | Unexpected failure | Unhandled exceptions |
| `503 Service Unavailable` | Service down | Database down, dependency failure |

---

## Standard Response Format

All API responses use a consistent wrapper:

### Success Response

```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "SOC Standard",
    "price": {
      "amount": 299.99,
      "currency": "EUR"
    },
    "status": "PUBLISHED"
  },
  "timestamp": "2026-03-05T10:30:00Z"
}
```

### Error Response

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed",
    "details": [
      {
        "field": "email",
        "message": "must be a valid email address"
      },
      {
        "field": "name",
        "message": "must not be blank"
      }
    ]
  },
  "timestamp": "2026-03-05T10:30:00Z"
}
```

### Java Implementation

```java
public record ApiResponse<T>(
    boolean success,
    T data,
    ApiError error,
    Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(ApiError error) {
        return new ApiResponse<>(false, null, error, Instant.now());
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message, null), Instant.now());
    }
}

public record ApiError(
    String code,
    String message,
    List<FieldError> details
) {}

public record FieldError(
    String field,
    String message
) {}
```

---

## Request / Response DTOs

### Request DTOs

Request DTOs live in `interfaces.rest.dto.request`:

```java
public record CreateProductRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    String name,

    @NotBlank(message = "Description is required")
    String description,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be positive")
    BigDecimal price,

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
    String currency
) {}
```

### Response DTOs

Response DTOs live in `interfaces.rest.dto.response`:

```java
public record ProductResponse(
    UUID id,
    String name,
    String description,
    PriceResponse price,
    String status,
    Instant createdAt
) {}

public record PriceResponse(
    BigDecimal amount,
    String currency
) {}
```

### Rules

| Rule | Rationale |
|------|-----------|
| Jakarta Validation annotations **only** on request DTOs | Domain must be annotation-free |
| Response DTOs never expose domain entities | Prevents model leakage |
| Use records for DTOs | Immutability, conciseness |
| All date/time fields use ISO 8601 format | Interoperability |
| UUIDs as strings in JSON | JSON does not have a UUID type |

---

## Query Parameters

### Filtering

```
GET /api/v1/products?status=PUBLISHED&category=edr
```

### Sorting

```
GET /api/v1/products?sort=price,asc
GET /api/v1/products?sort=createdAt,desc
```

### Searching

```
GET /api/v1/products?search=endpoint+detection
```

### Pagination

See [Pagination](pagination.md) for detailed conventions.

---

## Security: SQL Injection

All endpoints that accept `sort`, `filter`, or `search` must validate input against an allow-list and use parameter binding.
Do not build SQL or JPQL by string concatenation.

See [SQL Injection Protection](../security/sql-injection.md) for mandatory rules and examples.

---

## Security: XSS

All user-facing text fields must reject HTML content at the API boundary.
Use `@NoHtml` on request DTO fields and keep frontend rendering in text mode.

See [XSS Protection](../security/xss.md) for mandatory rules and examples.

---

## Security: HTTPS & Headers

Production environments must enforce HTTPS and standard security headers (HSTS, CSP, etc).
Configuration lives under `app.security` in `application.yml`.

See [HTTPS & Security Headers](../security/https-headers.md) for required settings.

---

## Security: CSRF

When using cookie-based authentication, CSRF protection must be enabled and the frontend
must send `X-CSRF-TOKEN` for unsafe requests.

See [CSRF Protection](../security/csrf.md) for required settings.

--- 

## Headers

### Request Headers

| Header | Required | Purpose |
|--------|----------|---------|
| `Authorization` | For protected endpoints | `Bearer <access_token>` |
| `Content-Type` | For POST/PUT/PATCH | `application/json` (default) or `multipart/form-data` for file upload endpoints |
| `Accept` | Optional | `application/json` (default) |
| `X-Request-Id` | Optional | Client-generated request correlation ID |

### Response Headers

| Header | Purpose |
|--------|---------|
| `Content-Type` | `application/json` |
| `X-Request-Id` | Echoed from request (or server-generated) |
| `X-RateLimit-Limit` | Maximum requests per window |
| `X-RateLimit-Remaining` | Remaining requests in current window |

---

## Caching

### Cache-Control Headers

| Resource Type | Strategy |
|--------------|----------|
| Product catalog | `Cache-Control: public, max-age=300` (5 min) |
| User-specific data | `Cache-Control: private, no-cache` |
| Static configuration | `Cache-Control: public, max-age=3600` (1 hour) |

### ETags

For resources that benefit from conditional requests:

```
GET /api/v1/products/123
→ ETag: "abc123"

GET /api/v1/products/123
If-None-Match: "abc123"
→ 304 Not Modified
```

---

## Rate Limiting

| Endpoint Type | Limit |
|--------------|-------|
| Public (unauthenticated) | 60 requests/minute |
| Authenticated | 300 requests/minute |
| Admin | 600 requests/minute |

Rate limit headers are included in every response.

---

## Controller Implementation Pattern

```java
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final Mediator mediator;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        var sortResult = ProductSort.parse(sort);
        if (sortResult.isFailure()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("INVALID_SORT", sortResult.getError()));
        }

        var query = new ListProductsQuery(page, size, status, category, search, sortResult.getValue());
        var result = mediator.send(query);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable UUID id) {
        var query = new GetProductByIdQuery(ProductId.of(id));
        var result = mediator.send(query);

        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {

        var command = new CreateProductCommand(
                request.name(), request.description(),
                new Money(request.price(), request.currency())
        );

        Result<ProductId> result = mediator.send(command);

        return result.fold(
                id -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(id.value())),
                error -> ResponseEntity.badRequest()
                        .body(ApiResponse.error("BAD_REQUEST", error))
        );
    }
}
```

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Verbs in URLs (`/api/v1/createProduct`) | Use HTTP methods: `POST /api/v1/products` |
| Returning domain entities directly | Map to response DTOs |
| Inconsistent response format | Always use `ApiResponse<T>` wrapper |
| Missing validation on request DTOs | Add Jakarta Validation annotations |
| Using numeric auto-increment IDs in URLs | Use UUIDs |
| Not including error details | Provide structured `ApiError` with field-level details |

---

## Related Documents

- [Error Handling](error-handling.md)
- [Pagination](pagination.md)
- [Authentication](../security/authentication.md)
- [Authorization](../security/authorization.md)
