# Pagination

## Purpose

This document defines the pagination strategy for the CYNA Platform API. All list endpoints must support pagination to ensure predictable performance and usable responses.

---

## Pagination Strategy

The CYNA Platform uses **offset-based pagination** as the primary strategy, with **cursor-based pagination** available for high-volume or real-time data streams.

### When to Use Each

| Strategy | When | Example |
|----------|------|---------|
| Offset-based | Standard list endpoints, admin dashboards | Product catalog, order list |
| Cursor-based | High-volume, real-time, or append-only data | Activity logs, notifications |

---

## Offset-Based Pagination

### Request Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | int | `0` | Page number (0-indexed) |
| `size` | int | `20` | Number of items per page |
| `sort` | string | varies | Sort field and direction |

**Constraints:**
- `page` >= 0
- `size` >= 1 and <= 100 (maximum)
- Default `size` = 20 if not specified

### Request Example

```
GET /api/v1/products?page=0&size=20&sort=createdAt,desc
GET /api/v1/products?page=2&size=10&sort=name,asc
```

### Response Format

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "name": "SOC Standard",
        "price": { "amount": 299.99, "currency": "EUR" }
      },
      {
        "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
        "name": "EDR Professional",
        "price": { "amount": 499.99, "currency": "EUR" }
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 47,
    "totalPages": 3,
    "first": true,
    "last": false
  },
  "timestamp": "2026-03-05T10:30:00Z"
}
```

### Response DTO

```java
public record PagedResponse<T>(
    List<T> items,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static <T> PagedResponse<T> of(List<T> items, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PagedResponse<>(
                items,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                page >= totalPages - 1
        );
    }
}
```

---

## Cursor-Based Pagination

### Request Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `cursor` | string | `null` | Opaque cursor from previous response |
| `limit` | int | `20` | Number of items to return |
| `direction` | string | `next` | `next` or `prev` |

### Request Example

```
GET /api/v1/notifications?limit=20
GET /api/v1/notifications?cursor=eyJpZCI6MTAwfQ&limit=20&direction=next
```

### Response Format

```json
{
  "success": true,
  "data": {
    "items": [ ... ],
    "nextCursor": "eyJpZCI6MTIwfQ",
    "previousCursor": "eyJpZCI6MTAwfQ",
    "hasNext": true,
    "hasPrevious": true
  },
  "timestamp": "2026-03-05T10:30:00Z"
}
```

### Cursor Implementation

Cursors are Base64-encoded JSON payloads:

```java
// Encoding
String cursor = Base64.getEncoder().encodeToString(
    """{"id":"%s","createdAt":"%s"}""".formatted(lastItem.getId(), lastItem.getCreatedAt()).getBytes()
);

// Decoding
byte[] decoded = Base64.getDecoder().decode(cursor);
CursorPayload payload = objectMapper.readValue(decoded, CursorPayload.class);
```

---

## Sorting

### Format

```
sort={field},{direction}
```

| Direction | Description |
|-----------|-------------|
| `asc` | Ascending |
| `desc` | Descending |

### Multiple Sort Fields

```
GET /api/v1/products?sort=status,asc&sort=createdAt,desc
```

### Allowed Sort Fields

Each endpoint documents its allowed sort fields. Attempting to sort by a non-allowed field returns `400 Bad Request`.

| Endpoint | Allowed Sort Fields |
|----------|-------------------|
| `GET /api/v1/products` | `name`, `price`, `createdAt`, `status` |
| `GET /api/v1/orders` | `createdAt`, `totalAmount`, `status` |
| `GET /api/v1/users` | `email`, `createdAt`, `role` |

---

## Backend Implementation

### Domain-Level Page Model

```java
package com.cyna.shared.domain;

public record Page<T>(
    List<T> items,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages
) {
    public boolean hasNext() {
        return pageNumber < totalPages - 1;
    }

    public boolean hasPrevious() {
        return pageNumber > 0;
    }
}
```

### Query

```java
public record ListProductsQuery(
    int page,
    int size,
    String status,
    String sort
) implements Query<PagedResponse<ProductReadModel>> {}
```

### Query Handler

```java
@Component
public class ListProductsQueryHandler implements QueryHandler<ListProductsQuery, PagedResponse<ProductReadModel>> {

    private final ProductReadRepository productReadRepository;

    @Override
    public PagedResponse<ProductReadModel> handle(ListProductsQuery query) {
        int safePage = Math.max(0, query.page());
        int safeSize = Math.min(Math.max(1, query.size()), 100);

        Page<ProductReadModel> page = productReadRepository.findAll(
                safePage, safeSize, query.status(), query.sort()
        );

        return PagedResponse.of(page.items(), page.pageNumber(), page.pageSize(), page.totalElements());
    }
}
```

### Controller

```java
@GetMapping
public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> listProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "createdAt,desc") String sort) {

    var query = new ListProductsQuery(page, size, status, sort);
    var result = mediator.send(query);

    return ResponseEntity.ok(ApiResponse.success(result));
}
```

---

## Frontend Implementation

### Angular Service

```typescript
export interface PagedResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);

  getProducts(page = 0, size = 20, sort = 'createdAt,desc'): Observable<ApiResponse<PagedResponse<Product>>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', sort);

    return this.http.get<ApiResponse<PagedResponse<Product>>>('/api/v1/products', { params });
  }
}
```

---

## Rules

| Rule | Rationale |
|------|-----------|
| All list endpoints must support pagination | Prevent unbounded result sets |
| Default page size is 20, maximum is 100 | Prevent excessive payloads |
| Page numbers are 0-indexed | Consistent with Spring Data convention |
| Always return total count and page metadata | Enables UI pagination controls |
| Validate sort fields against an allow-list | Prevent SQL injection and invalid sorts |
| Use cursor pagination for append-only/high-volume data | Better performance than offset for large datasets |

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Returning all items without pagination | Always paginate list endpoints |
| No maximum page size | Enforce `size <= 100` |
| Allowing sort by arbitrary fields | Validate against allow-list |
| Not returning total count | Include `totalElements` and `totalPages` |
| Using 1-indexed pages | Use 0-indexed pages consistently |
| Client not handling the "last page" case | Check `last` flag or `hasNext` |

---

## Related Documents

- [API Guidelines](api-guidelines.md)
- [Error Handling](error-handling.md)
