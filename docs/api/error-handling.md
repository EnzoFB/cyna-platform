# Error Handling

## Purpose

This document defines the error handling strategy for the CYNA Platform — from domain-level business errors to HTTP response formatting. Consistent error handling is critical for API consumers (frontend applications) and for debugging.

---

## Error Categories

| Category | HTTP Status | Source | Handling |
|----------|------------|--------|----------|
| Validation errors | 400 | Jakarta Validation on request DTOs | Automatic via `@Valid` |
| Authentication errors | 401 | JWT filter | Security filter chain |
| Authorization errors | 403 | RBAC checks | Security filter chain |
| Not found | 404 | Query returns null / empty | Controller logic |
| Business rule violations | 409 / 422 | Domain `Result.failure()` | Controller via `fold()` |
| Infrastructure errors | 500 | Database, external services | Global exception handler |

---

## Standard Error Response Format

All errors share the same structure:

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
      }
    ]
  },
  "timestamp": "2026-03-05T10:30:00Z"
}
```

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `success` | boolean | Yes | Always `false` for errors |
| `error.code` | string | Yes | Machine-readable error code |
| `error.message` | string | Yes | Human-readable error description |
| `error.details` | array | No | Field-level validation errors |
| `timestamp` | string | Yes | ISO 8601 timestamp |

---

## Error Codes

| Code | HTTP Status | Meaning |
|------|------------|---------|
| `VALIDATION_FAILED` | 400 | Request body validation failed |
| `INVALID_REQUEST` | 400 | Malformed request (bad JSON, missing params) |
| `UNAUTHORIZED` | 401 | Missing or invalid authentication |
| `TOKEN_EXPIRED` | 401 | JWT access token has expired |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `NOT_FOUND` | 404 | Requested resource does not exist |
| `CONFLICT` | 409 | Conflicting state (e.g., duplicate email) |
| `BUSINESS_RULE_VIOLATION` | 422 | Domain rule prevented the operation |
| `INTERNAL_ERROR` | 500 | Unexpected server error |
| `SERVICE_UNAVAILABLE` | 503 | Dependency unavailable |

---

## Implementation

### Global Exception Handler

A centralized exception handler catches all unhandled exceptions and formats them consistently:

```java
package com.cyna.shared.interfaces.rest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- Validation Errors (400) ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldError(e.getField(), e.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(
                ApiResponse.error(new ApiError("VALIDATION_FAILED", "Request validation failed", details))
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.error("INVALID_REQUEST", "Malformed request body")
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldError> details = ex.getConstraintViolations().stream()
                .map(v -> new FieldError(v.getPropertyPath().toString(), v.getMessage()))
                .toList();

        return ResponseEntity.badRequest().body(
                ApiResponse.error(new ApiError("VALIDATION_FAILED", "Constraint violation", details))
        );
    }

    // --- Not Found (404) ---

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error("NOT_FOUND", "Endpoint not found: " + ex.getRequestURL())
        );
    }

    // --- Method Not Allowed (405) ---

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
                ApiResponse.error("METHOD_NOT_ALLOWED", "HTTP method not supported: " + ex.getMethod())
        );
    }

    // --- Access Denied (403) ---

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.error("FORBIDDEN", "Insufficient permissions")
        );
    }

    // --- Catch-All (500) ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred")
        );
    }
}
```

### Business Errors in Controllers

Business errors from `Result.failure()` are mapped to HTTP responses in the controller:

```java
@PostMapping
public ResponseEntity<ApiResponse<UUID>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    var command = mapToCommand(request);
    Result<OrderId> result = mediator.send(command);

    return result.fold(
            orderId -> ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(orderId.value())),
            error -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponse.error("BUSINESS_RULE_VIOLATION", error))
    );
}
```

### Not Found Pattern

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable UUID id) {
    var query = new GetProductByIdQuery(ProductId.of(id));
    var result = mediator.send(query);

    if (result == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", "Product not found: " + id));
    }

    return ResponseEntity.ok(ApiResponse.success(result));
}
```

---

## Error Handling Flow

```
HTTP Request
     │
     ▼
┌──────────┐     Validation fails?     ┌─────────────────────────┐
│Controller │────────────────────────────▶│ 400 VALIDATION_FAILED   │
│ @Valid    │                             └─────────────────────────┘
└────┬─────┘
     │
     ▼
┌──────────┐     Result.failure()?      ┌─────────────────────────┐
│ Mediator  │────────────────────────────▶│ 422 BUSINESS_RULE_      │
│ Handler   │                             │     VIOLATION            │
└────┬─────┘                             └─────────────────────────┘
     │
     │ Result.success()
     ▼
┌──────────┐
│ 200/201  │
│ Success  │
└──────────┘

Uncaught Exception ──────────────────────▶ GlobalExceptionHandler
                                          │
                                          ▼
                                    ┌─────────────────┐
                                    │ 500 INTERNAL_    │
                                    │     ERROR        │
                                    └─────────────────┘
```

---

## Frontend Error Handling

The Angular frontend handles errors through its `ErrorInterceptor`:

```typescript
@Injectable()
export class ErrorInterceptor implements HttpInterceptor {

  private readonly notification = inject(NotificationService);

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        const apiError = error.error as ApiResponse<never>;

        switch (error.status) {
          case 400:
            this.notification.showValidationErrors(apiError.error.details);
            break;
          case 401:
            // Handled by RefreshTokenInterceptor
            break;
          case 403:
            this.notification.showError('You do not have permission to perform this action.');
            break;
          case 404:
            this.notification.showError('The requested resource was not found.');
            break;
          case 422:
            this.notification.showError(apiError.error.message);
            break;
          case 500:
            this.notification.showError('An unexpected error occurred. Please try again later.');
            break;
        }

        return throwError(() => error);
      })
    );
  }
}
```

---

## Rules

| Rule | Rationale |
|------|-----------|
| All errors use the `ApiResponse` wrapper | Consistent parsing on the client side |
| Never expose stack traces in API responses | Security — stack traces reveal internals |
| Never expose database error messages | Security — SQL error details are dangerous |
| Always include an error code | Machine-readable for client logic |
| Always include a human-readable message | User-facing display |
| Business errors return 422, not 400 | 400 = bad syntax; 422 = valid syntax, invalid semantics |
| Log all 500 errors with stack trace | Debugging in production |
| Do not log 400/422 errors as ERROR | They are expected client behavior |

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Returning raw exception messages to the client | Use structured `ApiError` with safe messages |
| Different error formats across endpoints | Always use `GlobalExceptionHandler` + `ApiResponse` |
| Throwing exceptions for business rules | Use `Result.failure()` |
| Not handling all error codes on the frontend | Cover 400, 401, 403, 404, 422, 500 |
| Exposing database constraint names in errors | Translate to user-friendly messages |
| Returning 200 for errors | Use appropriate HTTP status codes |

---

## Related Documents

- [API Guidelines](api-guidelines.md)
- [Result Pattern](../development/result-pattern.md)
- [Authentication](../security/authentication.md)
