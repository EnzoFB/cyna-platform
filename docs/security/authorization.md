# Authorization

## Purpose

This document describes the Role-Based Access Control (RBAC) authorization system of the CYNA Platform. It defines roles, permissions, and enforcement mechanisms.

---

## Authorization Model

The CYNA Platform uses **RBAC (Role-Based Access Control)**:
- Each user is assigned one or more **roles**.
- Each role grants access to a set of **permissions**.
- Permissions control access to API endpoints and operations.

```
User ──▶ Role(s) ──▶ Permission(s) ──▶ Endpoint Access
```

---

## Roles

| Role | Description | Target Users |
|------|-------------|-------------|
| `CUSTOMER` | Can browse products, place orders, manage own account | End-user customers |
| `ADMIN` | Full access to all backoffice operations | Platform administrators |
| `SUPPORT` | Read access to orders and users, limited write operations | Support staff |

### Role Hierarchy

```
ADMIN
  ├── includes all SUPPORT permissions
  └── includes all CUSTOMER permissions

SUPPORT
  └── includes read-only access to customer data

CUSTOMER
  └── self-service access only
```

---

## Permission Matrix

### Customer-Facing API

| Endpoint | CUSTOMER | SUPPORT | ADMIN |
|----------|----------|---------|-------|
| `GET /api/v1/products` | ✅ | ✅ | ✅ |
| `GET /api/v1/products/{id}` | ✅ | ✅ | ✅ |
| `POST /api/v1/orders` | ✅ | ❌ | ✅ |
| `GET /api/v1/orders` (own) | ✅ | ✅ | ✅ |
| `GET /api/v1/orders/{id}` (own) | ✅ | ✅ | ✅ |
| `POST /api/v1/orders/{id}/cancel` | ✅ (own) | ❌ | ✅ |
| `GET /api/v1/account` | ✅ | ❌ | ❌ |
| `PUT /api/v1/account` | ✅ | ❌ | ❌ |
| `POST /api/v1/auth/login` | Public | Public | Public |
| `POST /api/v1/auth/register` | Public | Public | Public |

### Backoffice API

| Endpoint | CUSTOMER | SUPPORT | ADMIN |
|----------|----------|---------|-------|
| `POST /api/v1/products` | ❌ | ❌ | ✅ |
| `PUT /api/v1/products/{id}` | ❌ | ❌ | ✅ |
| `DELETE /api/v1/products/{id}` | ❌ | ❌ | ✅ |
| `GET /api/v1/admin/orders` | ❌ | ✅ | ✅ |
| `GET /api/v1/admin/orders/{id}` | ❌ | ✅ | ✅ |
| `PUT /api/v1/admin/orders/{id}/status` | ❌ | ❌ | ✅ |
| `GET /api/v1/admin/users` | ❌ | ✅ | ✅ |
| `GET /api/v1/admin/users/{id}` | ❌ | ✅ | ✅ |
| `PUT /api/v1/admin/users/{id}` | ❌ | ❌ | ✅ |
| `POST /api/v1/admin/users/{id}/deactivate` | ❌ | ❌ | ✅ |

---

## Implementation

### Spring Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Customer endpoints
                        .requestMatchers("/api/v1/orders/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/api/v1/account/**").hasRole("CUSTOMER")

                        // Admin endpoints
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPPORT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")

                        // Default — deny all
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                        .accessDeniedHandler(new JwtAccessDeniedHandler())
                )
                .build();
    }
}
```

### Custom Authentication Entry Point

Returns a structured JSON error instead of Spring's default HTML:

```java
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        ApiResponse<Void> error = ApiResponse.error("UNAUTHORIZED", "Authentication required");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
```

### Custom Access Denied Handler

```java
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");

        ApiResponse<Void> error = ApiResponse.error("FORBIDDEN", "Insufficient permissions");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
```

### Method-Level Security (Optional)

For fine-grained access control within handlers:

```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping
public ResponseEntity<ApiResponse<UUID>> createProduct(@Valid @RequestBody CreateProductRequest request) {
    // ...
}
```

### Resource Ownership Checks

For endpoints where users can only access their own resources:

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
        @PathVariable UUID id,
        @AuthenticationPrincipal String currentUserId) {

    var query = new GetOrderByIdQuery(OrderId.of(id));
    var order = mediator.send(query);

    if (order == null) {
        return ResponseEntity.notFound().build();
    }

    // Ownership check — customers can only view their own orders
    if (!order.customerId().equals(currentUserId)
            && !hasRole(SecurityContextHolder.getContext(), "ADMIN")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("FORBIDDEN", "Access denied"));
    }

    return ResponseEntity.ok(ApiResponse.success(order));
}
```

---

## Frontend Authorization

### Route Guards

```typescript
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  router.navigate(['/auth/login'], { queryParams: { returnUrl: state.url } });
  return false;
};

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const requiredRoles = route.data['roles'] as string[];

  if (!requiredRoles || requiredRoles.length === 0) {
    return true;
  }

  const userRoles = authService.getUserRoles();
  return requiredRoles.some(role => userRoles.includes(role));
};
```

### Route Configuration

```typescript
export const routes: Routes = [
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component'),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN', 'SUPPORT'] }
  },
  {
    path: 'products/new',
    loadComponent: () => import('./features/products/product-form.component'),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] }
  }
];
```

### UI Conditional Rendering

```html
@if (authService.hasRole('ADMIN')) {
  <button (click)="deleteProduct()">Delete Product</button>
}
```

**Important**: Frontend role checks are for **UX only**. The backend **always** enforces authorization regardless of frontend checks.

---

## Security Rules

| Rule | Rationale |
|------|-----------|
| Default deny — all endpoints require authentication unless explicitly public | Secure by default |
| Backend always enforces authorization | Frontend checks are bypassable |
| Use role-based checks, not user-ID checks, for admin endpoints | Scalability |
| Implement resource ownership for user-specific data | Privacy and data isolation |
| Log all authorization failures | Security monitoring |
| Never expose role names in error messages | Information disclosure |
| Roles are stored in JWT claims | Avoid database lookups on every request |
| Role changes require re-authentication (new JWT) | Consistency |
| Admin endpoints use a separate URL prefix (`/api/v1/admin/`) | Clear separation |

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Relying only on frontend role checks | Always enforce on the backend |
| Forgetting to check resource ownership | Add ownership validation for user-scoped resources |
| Using `permitAll()` as default | Use `authenticated()` as default, whitelist public endpoints |
| Storing role as a string without validation | Use an enum |
| Not logging authorization failures | Log all 403 responses for security auditing |
| Allowing role escalation via API | Validate role changes server-side |

---

## Related Documents

- [Authentication](authentication.md)
- [API Guidelines](../api/api-guidelines.md)
- [Backend Architecture](../architecture/backend-architecture.md)
