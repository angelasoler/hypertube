# CSRF Protection Strategy

## Current Implementation: CSRF Protection Disabled for Stateless API

### Why CSRF Protection is Disabled

This API Gateway serves a **stateless RESTful API** using **JWT (JSON Web Token)** authentication. CSRF protection has been intentionally disabled for the following architectural and security reasons:

### 1. Stateless Architecture
- The API does not use cookies or session-based authentication
- JWT tokens are stored in client-side storage (localStorage/sessionStorage)
- Tokens are sent via `Authorization: Bearer <token>` header
- No session state is maintained on the server

### 2. JWT in Headers (Not Cookies)
CSRF attacks exploit the automatic inclusion of cookies by browsers. Our implementation:
- Does NOT store JWT tokens in cookies
- Requires explicit JavaScript to add the `Authorization` header
- Prevents automatic token inclusion in cross-site requests

### 3. SameSite Cookie Protection Not Needed
Since we don't use cookies for authentication:
- No need for `SameSite=Strict` or `SameSite=Lax` attributes
- CSRF tokens would provide no additional security value
- The attack vector CSRF protects against doesn't exist in our architecture

## Security Measures in Place

### Instead of CSRF Protection, we implement:

1. **JWT Token Authentication**
   - All state-changing operations require valid JWT
   - Tokens must be explicitly added to requests
   - Cannot be automatically included by browser

2. **CORS (Cross-Origin Resource Sharing)**
   - Configured to only allow requests from trusted origins
   - Prevents unauthorized cross-origin requests
   - See `CorsConfiguration` for details

3. **Issuer and Audience Validation**
   - JWT tokens validated for correct issuer (`hypertube`)
   - Audience validation ensures tokens are for this API
   - Prevents token reuse from other systems

4. **Rate Limiting**
   - Prevents brute force attacks
   - Limits request frequency per client
   - See rate limiting configuration in `application.yml`

5. **Short-Lived Tokens**
   - Access tokens expire quickly (1 hour default)
   - Reduces window of opportunity for token theft
   - Refresh token mechanism for legitimate users

## When CSRF Protection WOULD Be Required

CSRF protection should be enabled if any of the following changes occur:

1. **Session-Based Authentication Added**
   - If we add cookie-based sessions alongside JWT
   - If authentication state is stored in server-side sessions

2. **JWT Stored in Cookies**
   - If tokens are moved from headers to cookies
   - Even with `httpOnly` flag, CSRF protection needed

3. **OAuth2 Cookie Flow**
   - If OAuth2 authorization code flow stores state in cookies
   - Current implementation uses state parameter only

4. **Mixed Authentication**
   - If the application serves both a traditional web UI (with sessions) and API
   - Different routes may require different protection strategies

## Monitoring and Review

This decision should be reviewed if:
- Architecture changes from stateless to stateful
- New authentication methods are added
- Frontend changes how it stores/sends tokens
- Security audit recommends changes

## References

- [OWASP CSRF Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html)
- [JWT and CSRF - When You Need It](https://stackoverflow.com/questions/21357182/csrf-token-necessary-when-using-stateless-sessionless-authentication)
- [Spring Security CSRF Documentation](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)

## Last Reviewed
Date: 2025-11-25
Reviewer: Technical Debt Resolution Specialist
Status: Appropriate for current stateless JWT architecture
