# Authentication

## Purpose

This document describes the authentication system of the CYNA Platform, based on **JWT (JSON Web Tokens)** with **access/refresh token rotation**. It covers the token lifecycle, security measures, and implementation guidelines.

---

## Overview

The CYNA Platform uses a **stateless JWT-based authentication** system:

- **Access Token** — Short-lived JWT for API authorization (15 minutes).
- **Refresh Token** — Long-lived opaque token for obtaining new access tokens (7 days).

```
┌─────────┐                           ┌──────────┐
│  Client │                           │  Backend │
└────┬────┘                           └─────┬────┘
     │                                      │
     │  1. POST /auth/login                 │
     │  { email, password }                 │
     │─────────────────────────────────────▶│
     │                                      │ Validate credentials
     │  2. { accessToken, refreshToken }    │ Generate tokens
     │◀─────────────────────────────────────│
     │                                      │
     │  3. GET /api/v1/products             │
     │  Authorization: Bearer <accessToken> │
     │─────────────────────────────────────▶│
     │                                      │ Validate JWT
     │  4. { data: [...] }                  │
     │◀─────────────────────────────────────│
     │                                      │
     │  ... access token expires ...        │
     │                                      │
     │  5. POST /auth/refresh               │
     │  { refreshToken }                    │
     │─────────────────────────────────────▶│
     │                                      │ Validate refresh token
     │  6. { accessToken, refreshToken }    │ Rotate both tokens
     │◀─────────────────────────────────────│
     │                                      │
```

---

## Token Specifications

### Access Token

| Property | Value |
|----------|-------|
| Type | JWT (JWS, signed) |
| Algorithm | HS256 (HMAC-SHA256) or RS256 (RSA) |
| Expiration | 15 minutes |
| Storage (frontend) | In-memory (JavaScript variable) |
| Transmitted via | `Authorization: Bearer <token>` header |

**JWT Payload (Claims):**

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "roles": ["CUSTOMER"],
  "iat": 1709622600,
  "exp": 1709623500,
  "iss": "cyna-platform",
  "jti": "unique-token-id"
}
```

| Claim | Type | Description |
|-------|------|-------------|
| `sub` | UUID | User ID |
| `email` | string | User's email |
| `roles` | string[] | User's roles for RBAC |
| `iat` | number | Issued at (Unix timestamp) |
| `exp` | number | Expiration (Unix timestamp) |
| `iss` | string | Issuer identifier |
| `jti` | string | Unique token ID (for revocation) |

### Refresh Token

| Property | Value |
|----------|-------|
| Type | Opaque (random UUID or secure random string) |
| Expiration | 7 days |
| Storage (backend) | Database (hashed), associated with user |
| Storage (frontend) | HttpOnly secure cookie |
| Rotation | Yes — each use generates a new refresh token |

---

## Authentication Endpoints

### Login

```
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecureP@ssw0rd"
}
```

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "d290f1ee-6c54-4b01-90e6-d701748f0851",
    "expiresIn": 900,
    "tokenType": "Bearer"
  },
  "timestamp": "2026-03-05T10:30:00Z"
}
```

**Failure Response (401):**

```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Invalid email or password"
  },
  "timestamp": "2026-03-05T10:30:00Z"
}
```

### Refresh Token

```
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "d290f1ee-6c54-4b01-90e6-d701748f0851"
}
```

**Success Response (200):**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "new-refresh-token-uuid",
    "expiresIn": 900,
    "tokenType": "Bearer"
  },
  "timestamp": "2026-03-05T10:30:00Z"
}
```

**Key behavior**: The old refresh token is **invalidated** and a new one is issued. This is **refresh token rotation**.

### Logout

```
POST /api/v1/auth/logout
Authorization: Bearer <accessToken>

{
  "refreshToken": "current-refresh-token"
}
```

Invalidates the refresh token. The access token will expire naturally.

### Register

```
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "newuser@example.com",
  "password": "SecureP@ssw0rd",
  "firstName": "John",
  "lastName": "Doe"
}
```

---

## Backend Implementation

### JWT Service

```java
@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getId().value().toString())
                .claim("email", user.getEmail().value())
                .claim("roles", user.getRoles().stream().map(Role::name).toList())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusMillis(accessTokenExpiration)))
                .issuer("cyna-platform")
                .id(UUID.randomUUID().toString())
                .signWith(getSigningKey())
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

### JWT Authentication Filter

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.validateToken(token);
            String userId = claims.getSubject();
            List<String> roles = claims.get("roles", List.class);

            var authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();

            var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException e) {
            // Token invalid or expired — do nothing, request proceeds unauthenticated
        }

        filterChain.doFilter(request, response);
    }
}
```

### Refresh Token Storage

Refresh tokens are stored **hashed** in the database:

```java
@Entity
@Table(name = "refresh_tokens", schema = "user_schema")
public class RefreshTokenJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash; // SHA-256 hash of the refresh token

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
```

---

## Refresh Token Rotation

### Why Rotation?

If a refresh token is stolen, rotation limits the window of exploitation:

1. Attacker uses stolen refresh token → gets new token pair.
2. Legitimate user tries to use the old refresh token → **fails** (it's already consumed).
3. System detects reuse → **revoke all refresh tokens for the user** (breach response).

### Implementation

```java
public AuthTokens refreshToken(String refreshToken) {
    String tokenHash = hashToken(refreshToken);
    RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));

    if (stored.isRevoked()) {
        // Possible token theft — revoke ALL tokens for this user
        refreshTokenRepository.revokeAllByUserId(stored.getUserId());
        throw new AuthenticationException("Refresh token reuse detected");
    }

    if (stored.getExpiresAt().isBefore(Instant.now())) {
        throw new AuthenticationException("Refresh token expired");
    }

    // Revoke old token
    stored.revoke();
    refreshTokenRepository.save(stored);

    // Issue new token pair
    User user = userRepository.findById(stored.getUserId()).orElseThrow();
    String newAccessToken = jwtService.generateAccessToken(user);
    String newRefreshToken = generateRefreshToken(user.getId());

    return new AuthTokens(newAccessToken, newRefreshToken);
}
```

---

## Frontend Token Management

### Angular AuthService

```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {
  private accessToken = signal<string | null>(null);

  login(email: string, password: string): Observable<void> {
    return this.http.post<ApiResponse<AuthTokens>>('/api/v1/auth/login', { email, password })
      .pipe(
        tap(response => {
          this.accessToken.set(response.data.accessToken);
          this.storeRefreshToken(response.data.refreshToken);
        }),
        map(() => void 0)
      );
  }

  getAccessToken(): string | null {
    return this.accessToken();
  }

  private storeRefreshToken(token: string): void {
    // Store in HttpOnly cookie via a SET-COOKIE header from the server
    // OR in localStorage (less secure but simpler)
  }
}
```

### Token Refresh Interceptor

```typescript
@Injectable()
export class RefreshTokenInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshSubject = new BehaviorSubject<string | null>(null);

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 && !req.url.includes('/auth/')) {
          return this.handle401Error(req, next);
        }
        return throwError(() => error);
      })
    );
  }

  private handle401Error(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshSubject.next(null);

      return this.authService.refreshToken().pipe(
        switchMap(tokens => {
          this.isRefreshing = false;
          this.refreshSubject.next(tokens.accessToken);
          return next.handle(this.addToken(req, tokens.accessToken));
        }),
        catchError(err => {
          this.isRefreshing = false;
          this.authService.logout();
          return throwError(() => err);
        })
      );
    }

    return this.refreshSubject.pipe(
      filter(token => token !== null),
      take(1),
      switchMap(token => next.handle(this.addToken(req, token!)))
    );
  }
}
```

---

## Security Rules

| Rule | Rationale |
|------|-----------|
| Access tokens expire in 15 minutes | Limits damage if token is stolen |
| Refresh tokens expire in 7 days | Balance between security and UX |
| Refresh tokens are rotated on each use | Detects token theft |
| Refresh tokens are stored hashed in the database | Prevents exposure in case of DB breach |
| Access tokens are stored in memory only (not localStorage) | Prevents XSS token theft |
| JWT secret must be at least 256 bits | HMAC-SHA256 requirement |
| Never include sensitive data in JWT payload | JWTs are base64-encoded, not encrypted |
| Passwords are hashed with bcrypt (cost 12+) | Industry standard |
| Rate-limit login attempts | Prevents brute force |
| Lock account after 5 failed login attempts | Prevents brute force |

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Storing access tokens in localStorage | Store in memory; use refresh tokens for persistence |
| Not rotating refresh tokens | Always issue a new refresh token on use |
| Not hashing refresh tokens in the database | Use SHA-256 before storing |
| Long-lived access tokens (hours/days) | Keep to 15 minutes maximum |
| Including password in JWT claims | Never include sensitive data |
| Not invalidating refresh tokens on logout | Delete/revoke on logout |
| Missing rate limiting on auth endpoints | Implement rate limiting |

---

## Related Documents

- [Authorization](authorization.md)
- [API Guidelines](../api/api-guidelines.md)
- [Error Handling](../api/error-handling.md)
