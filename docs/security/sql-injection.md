# SQL Injection Protection

## Purpose

This document defines the SQL injection protection rules for the CYNA Platform.
It applies to every backend module and is mandatory for all list, search, and filter endpoints.

---

## Rules

1. Never build SQL or JPQL by string concatenation with user input.
2. Always use parameter binding for query values.
3. Sorting must use an allow-list of fields. Unknown fields return `400 Bad Request`.
4. Prefer JPA Criteria / Specifications for dynamic filters.
5. Avoid native SQL. If native SQL is required, use named parameters and strict input validation.
6. Do not expose database error details in API responses.

---

## Approved Patterns

### Allow-listed Sorting

Example of safe sort parsing with an allow-list:

```java
var sortResult = ProductSort.parse(sort);
if (sortResult.isFailure()) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("INVALID_SORT", sortResult.getError()));
}
```

### Dynamic Filters with Criteria

```java
if (search != null && !search.isBlank()) {
    String likeValue = "%" + search.toLowerCase() + "%";
    predicates.add(cb.like(cb.lower(root.get("name")), likeValue));
}
```

---

## Prohibited Patterns

```java
// BAD: string concatenation with user input
String q = "SELECT * FROM products WHERE name = '" + name + "'";

// BAD: dynamic ORDER BY without allow-list
String q = "SELECT * FROM products ORDER BY " + sort;
```

---

## Review Checklist

- Input used in queries is parameterized
- Sort fields are allow-listed
- No raw SQL with concatenated user input
- Error responses do not leak SQL details

---

## Related Documents

- [API Guidelines](../api/api-guidelines.md)
- [Pagination](../api/pagination.md)
- [Error Handling](../api/error-handling.md)
