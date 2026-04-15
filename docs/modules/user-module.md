# User (IAM) Module

## Purpose

This document describes the **User (Identity & Access Management)** module of the CYNA Platform. This module manages user registration, authentication (JWT), session management (refresh tokens), and user account lifecycle.

---

## Overview

| Property | Value |
|----------|-------|
| Module name | `user` |
| Bounded context | Identity & Access Management (IAM) |
| Base package | `com.cyna.modules.user` |
| Database schema | `user_schema` |
| Primary aggregate | `User` |

The User module is the central identity provider for the platform. It issues JWT access tokens and refresh tokens, enforces role-based access control (RBAC), and publishes domain events consumed by the Notification module. All other modules depend on it for user identity.

---

## Domain Model

### Aggregate: User

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Unique user identifier |
| `email` | `Email` | User email address (unique, validated format) |
| `hashedPassword` | `HashedPassword` | BCrypt-hashed password |
| `firstName` | `String` | First name |
| `lastName` | `String` | Last name |
| `role` | `Role` | User role (default: `CUSTOMER`) |
| `status` | `UserStatus` | Account status (default: `ACTIVE`) |
| `createdAt` | `Instant` | Account creation timestamp |
| `updatedAt` | `Instant` | Last modification timestamp |

**Behaviors:**

| Method | Description |
|--------|-------------|
| `register(email, hashedPassword, firstName, lastName, lang)` | Factory — creates a new user with role `CUSTOMER` and status `ACTIVE`, raises `UserRegistered` |
| `deactivate(reason)` | Returns a new deactivated User instance, raises `UserDeactivated` |

### Entity: RefreshToken

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Unique token identifier |
| `userId` | `UUID` | Reference to the user |
| `tokenHash` | `String` | SHA-256 hash of the refresh token |
| `expiresAt` | `Instant` | Expiration timestamp |
| `revoked` | `boolean` | Whether the token has been revoked |
| `createdAt` | `Instant` | Creation timestamp |

### Value Objects & Enums

| Value Object | Description | Validation Rules |
|-------------|-------------|------------------|
| `Email` | User email address | Non-blank, must match email regex pattern |
| `HashedPassword` | BCrypt hash of the password | Non-blank |
| `Role` | User role | Enum: `CUSTOMER`, `ADMIN`, `SUPPORT` |
| `UserStatus` | Account status | Enum: `ACTIVE`, `INACTIVE` |

### Port Interfaces

| Port | Methods | Description |
|------|---------|-------------|
| `JwtProvider` | `generateAccessToken(user)`, `generateRefreshToken()`, `validateToken(token)`, `extractUserId(token)` | JWT token generation and validation |
| `PasswordHasher` | `hash(rawPassword)`, `matches(rawPassword, hashedPassword)` | Password hashing (BCrypt) |

### Repository Interfaces

**`UserRepository`:**

| Method | Description |
|--------|-------------|
| `save(User)` | Persist a user |
| `findById(UUID)` | Find user by ID |
| `findByEmail(Email)` | Find user by email |
| `existsByEmail(Email)` | Check if email is already taken |

**`RefreshTokenRepository`:**

| Method | Description |
|--------|-------------|
| `save(RefreshToken)` | Persist a refresh token |
| `findByTokenHash(String)` | Find token by its hash |
| `revokeAllByUserId(UUID)` | Revoke all refresh tokens for a user |

---

## Domain Events

| Event | Trigger | Payload |
|-------|---------|---------|
| `UserRegistered` | User creates an account | userId, email, firstName, role, lang, occurredAt |
| `UserDeactivated` | Admin deactivates a user | userId, reason, occurredAt |

---

## Application Layer

### Commands

#### `RegisterUserCommand` → `RegisterUserCommandHandler`

Registers a new user account and returns JWT tokens.

| Field | Type | Validation |
|-------|------|------------|
| `email` | `String` | Required, valid email format |
| `password` | `String` | Required, min 8 characters |
| `firstName` | `String` | Required, non-blank, no HTML |
| `lastName` | `String` | Required, non-blank, no HTML |
| `lang` | `String` | Required, non-blank |

**Returns:** `Result<AuthTokens>` — accessToken, refreshToken, expiresIn

**Flow:** Validate email uniqueness → hash password → create User → save → publish `UserRegistered` → generate JWT tokens → save refresh token → return tokens.

#### `LoginCommand` → `LoginCommandHandler`

Authenticates a user by email and password, returns JWT tokens.

| Field | Type | Validation |
|-------|------|------------|
| `email` | `String` | Required, valid email |
| `password` | `String` | Required, non-blank |

**Returns:** `Result<AuthTokens>` — accessToken, refreshToken, expiresIn

**Flow:** Find user by email → verify password → check user status is `ACTIVE` → generate JWT tokens → save refresh token → return tokens.

#### `RefreshTokenCommand` → `RefreshTokenCommandHandler`

Refreshes an expired access token using a valid refresh token (token rotation).

| Field | Type | Validation |
|-------|------|------------|
| `refreshToken` | `String` | Required, non-blank |

**Returns:** `Result<AuthTokens>` — new accessToken, new refreshToken, expiresIn

**Flow:** Hash incoming token → find by hash → validate not expired/revoked → revoke old token → generate new token pair → save new refresh token → return tokens.

#### `LogoutCommand` → `LogoutCommandHandler`

Logs out the user by revoking all refresh tokens.

| Field | Type | Validation |
|-------|------|------------|
| `userId` | `UUID` | Required |

**Returns:** `Result<Void>`

**Flow:** Revoke all refresh tokens for the user.

### Queries

#### `GetCurrentUserQuery` → `GetCurrentUserQueryHandler`

Returns the profile of the authenticated user.

| Field | Type |
|-------|------|
| `userId` | `UUID` |

**Returns:** `UserReadModel` — id, email, firstName, lastName, role, createdAt

#### `CheckEmailQuery` → `CheckEmailQueryHandler`

Checks if an email is already registered.

| Field | Type |
|-------|------|
| `email` | `String` |

**Returns:** `boolean`

---

## REST Endpoints

### Authentication (Public)

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| `POST` | `/api/v1/auth/register` | Public | Register a new user account |
| `POST` | `/api/v1/auth/login` | Public | Login with email and password |
| `POST` | `/api/v1/auth/refresh` | Public | Refresh JWT tokens |
| `POST` | `/api/v1/auth/logout` | CUSTOMER | Logout (revoke all refresh tokens) |

### Account (Authenticated)

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/account` | CUSTOMER | Get current user profile |
| `GET` | `/api/v1/account/check-email` | Public | Check if email is already taken |

### Backoffice (Admin)

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/admin/users` | ADMIN, SUPPORT | List all users |
| `GET` | `/api/v1/admin/users/{id}` | ADMIN, SUPPORT | Get any user details |
| `PUT` | `/api/v1/admin/users/{id}` | ADMIN | Update user details |
| `POST` | `/api/v1/admin/users/{id}/deactivate` | ADMIN | Deactivate a user account |

### Request Bodies

**`POST /api/v1/auth/register`** — Register

| Field | Type | Validation |
|-------|------|------------|
| `email` | `String` | Required, valid email format |
| `password` | `String` | Required, min 8 characters |
| `firstName` | `String` | Required, non-blank, no HTML |
| `lastName` | `String` | Required, non-blank, no HTML |
| `lang` | `String` | Required, non-blank |

**`POST /api/v1/auth/login`** — Login

| Field | Type | Validation |
|-------|------|------------|
| `email` | `String` | Required, valid email |
| `password` | `String` | Required, non-blank |

**`POST /api/v1/auth/refresh`** — Refresh Token

| Field | Type | Validation |
|-------|------|------------|
| `refreshToken` | `String` | Required, non-blank |

### Response Bodies

**`AuthResponse`** (register, login, refresh)

| Field | Type |
|-------|------|
| `accessToken` | `String` |
| `refreshToken` | `String` |
| `expiresIn` | `long` |
| `tokenType` | `String` (`"Bearer"`) |

**`UserResponse`** (account, admin user endpoints)

| Field | Type |
|-------|------|
| `id` | `UUID` |
| `email` | `String` |
| `firstName` | `String` |
| `lastName` | `String` |
| `role` | `String` |
| `createdAt` | `Instant` |

---

## Security

### JWT Configuration

| Property | Value |
|----------|-------|
| Access token lifetime | 15 minutes |
| Refresh token lifetime | 7 days |
| Token rotation | Enabled (old refresh token revoked on use) |
| Algorithm | HMAC-SHA256 |
| JWT claims | `sub` (userId), `email`, `roles` |

### Infrastructure Adapters

| Adapter | Implements | Description |
|---------|-----------|-------------|
| `JwtProviderImpl` | `JwtProvider` | JWT generation/validation using HMAC-SHA256 |
| `BcryptPasswordHasher` | `PasswordHasher` | BCrypt password hashing |
| `JwtAuthenticationFilter` | Spring Security filter | Extracts and validates JWT from `Authorization` header |
| `JwtAuthenticationEntryPoint` | `AuthenticationEntryPoint` | Returns 401 for unauthenticated requests |
| `JwtAccessDeniedHandler` | `AccessDeniedHandler` | Returns 403 for unauthorized requests |

---

## Module Dependencies

| Direction | Module | Mechanism | Description |
|-----------|--------|-----------|-------------|
| **Provides to** | All modules | User identity | All modules reference user identity via `UUID` |
| **Provides to** | Notification | Domain events (async) | `UserRegistered`, `UserDeactivated` trigger notifications |
| **Provides to** | Order, Cart, Subscription | RBAC | Spring Security enforces role-based access |

---

## Business Rules

| Rule | Enforcement |
|------|-------------|
| Email must be unique | Application layer check + DB unique constraint |
| Email must match a valid format | `Email` value object regex validation |
| Password must be at least 8 characters | Request DTO validation (`@Size(min=8)`) |
| First/last name must not contain HTML | `@NoHtml` custom validation annotation |
| Only active users can log in | Login handler checks `UserStatus.ACTIVE` |
| Refresh tokens are rotated on use | Old token revoked, new pair generated |
| Logout revokes all refresh tokens | `revokeAllByUserId()` |
| Only ADMIN can deactivate users | Spring Security + RBAC |
| Default role is CUSTOMER | Set in `User.register()` factory |

---

## Related Documents

- [Authentication](../security/authentication.md)
- [Authorization](../security/authorization.md)
- [Bounded Contexts](../domain/bounded-contexts.md)
- [Module Structure](../architecture/module-structure.md)
