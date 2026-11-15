# Auth token

This API does not create auth tokens. The auth service signs a JWT, and the browser sends it on each API request. This app checks the signature and the claims, then allows the request only when the user is `ACTIVE`.

## Request header

```
Authorization: Bearer <jwt>
```

The word `Bearer` and the token are separated by a single space. This API does not read the token from a cookie.

`GET /api`, `GET /api/ping`, and `POST /api/webhooks/users` stay public. Every other `/api/**` route, including `GET /api/categories`, requires this header.

## What the token contains

JOSE header:

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

Claims:

| Claim | Meaning |
| --- | --- |
| `sub` | User id as a UUID string. This is the same id the auth service posts to `POST /api/webhooks/users`. A missing `sub`, or one that is not a UUID, is rejected. |
| `iat` | Issued-at time, as NumericDate seconds. Set this when creating the token. |
| `exp` | Expiry time, as NumericDate seconds. Use about 15 minutes. A missing or past `exp` is rejected. |

Example payload:

```json
{
  "sub": "6f1c0c2e-3b1a-4c2d-9e5f-1a2b3c4d5e6f",
  "iat": 1730000000,
  "exp": 1730000900
}
```

The signature is HMAC-SHA256 with the shared secret `app.auth.jwt-secret`. The secret must be at least 32 bytes. The auth service and this API must use the same value. The value in `application.properties` is for local development; override it in any shared environment.

`alg` must be `HS256`. `none` and any other algorithm are rejected.

Before a token will work, the auth service must already have synced that user through `POST /api/webhooks/users`, and `account_status` must be `ACTIVE`. A valid signature for a missing, `PENDING`, `INACTIVE`, or `DELETED` user is still rejected.

## How the auth service creates the token

Sign with the same library this API uses to check the token, `NimbusJwtEncoder` from `spring-security-oauth2-jose`:

```java
SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
JwtEncoder encoder = NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
Instant now = Instant.now();
JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
JwtClaimsSet claims = JwtClaimsSet.builder()
    .subject(userId.toString())
    .issuedAt(now)
    .expiresAt(now.plus(Duration.ofMinutes(15)))
    .build();
String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
```

`secret` is the shared `app.auth.jwt-secret`. `userId` is the user's UUID.

## Category list

`GET /api/categories` with a valid token returns that user's categories in an `ApiResponse`. `DELETED` rows are left out. Rows are ordered by name, then id. Another user's categories are not included.

The response fields are `id`, `name`, `description`, `parentId`, `status`, `createdAt`, and `updatedAt`.

A token whose `exp` is in the past returns `401` with `{ "message": "Token expired" }`.

These cases return `401` with `{ "success": false, "message": "Authentication required" }`:

- the header is missing or the token cannot be parsed
- the signature does not match the shared secret
- `exp` is missing
- `sub` is not a UUID
- no user with that id exists, or the user is not `ACTIVE`
