  # AGENTS.md

This file provides guidance for agentic coding assistants working in the HyperTube codebase.

## Project Overview

HyperTube is a microservices-based video streaming platform using BitTorrent protocol. The architecture consists of:
- **Backend**: Spring Boot Cloud microservices (Java 17, Maven)
- **Frontend**: Nuxt 3 + Vue 3 + TypeScript
- **Infrastructure**: PostgreSQL, Redis, RabbitMQ, Docker
- **No linting/formatting tools configured** - follow patterns in existing code

## Build & Run Commands

### Environment Setup
```bash
make env-setup                 # Create .env from template
make auto-configure-env        # Generate secure passwords automatically
make security-check            # Validate security configuration
```

### Docker Operations
```bash
make up                        # Start all services (detached)
make up-build                  # Build and start all services
make down                      # Stop all services
make restart                   # Restart all services
make logs                      # View all service logs
make logs-<service>            # View specific service logs (gateway, users, streaming, search)
make ps                        # Show service status
make health                    # Check health of all services
```

### Backend Build (Maven)
```bash
# Build all services
make build-services            # Maven clean package for all services
make build-gateway             # Build API Gateway only
make build-users               # Build User Management only

# Build specific service directly
cd services/<service-name>
mvn clean package              # Build with tests
mvn clean package -DskipTests  # Build without tests
mvn clean install              # Install to local Maven repo
```

### Backend Testing
```bash
# Run all tests
make test-all                  # Test all services

# Run specific service tests
make test-gateway              # Test API Gateway
make test-users                # Test User Management
cd services/<service-name> && mvn test  # Test specific service

# Run single test class
cd services/<service-name>
mvn test -Dtest=UserServiceTest

# Run single test method
mvn test -Dtest=UserServiceTest#testRegisterUser

# Run tests with coverage (if configured)
mvn test jacoco:report
```

### Frontend Operations
```bash
cd frontend
npm install                    # Install dependencies
npm run dev                    # Start dev server (http://localhost:3000)
npm run build                  # Build for production
npm run preview                # Preview production build
npm run generate               # Generate static site
```

### Linting & Formatting
**No linters configured** - Follow existing code patterns:
- Backend: 4-space indentation, K&R brace style
- Frontend: 2-space indentation (Nuxt default)

### Database & Cache
```bash
make db-shell                  # Open PostgreSQL shell
make redis-cli                 # Open Redis CLI
make redis-flush               # Clear all Redis cache (WARNING: destructive)
make rabbitmq-ui               # Open RabbitMQ management UI (http://localhost:15672)
```

### Cleanup
```bash
make clean                     # Remove build artifacts (target/ directories)
make clean-docker              # Remove Docker images and containers
make clean-all                 # Full cleanup (artifacts + Docker + volumes)
```

## Code Style Guidelines

### Backend (Java/Spring Boot)

#### Import Organization (STRICT ORDER)
1. Package declaration
2. Blank line
3. Project imports (`com.hypertube.*`)
4. Jakarta imports (`jakarta.*`)
5. Lombok imports (`lombok.*`)
6. Spring imports (`org.springframework.*`)
7. Java standard library (`java.*`)
8. Blank line before class

Example:
```java
package com.hypertube.user.controller;

import com.hypertube.user.dto.AuthResponse;
import com.hypertube.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
```

#### File Naming & Package Structure
- Controllers: `*Controller.java` in `controller/` package
- Services: `*Service.java` in `service/` package
- Repositories: `*Repository.java` in `repository/` package (interfaces only)
- Entities: Plain nouns in `entity/` package (e.g., `User`, `Video`)
- DTOs: `*DTO.java`, `*Request.java`, `*Response.java` in `dto/` package
- Configs: `*Config.java` in `config/` package

#### Formatting & Style
- **Indentation**: 4 spaces (not tabs)
- **Brace placement**: K&R style (opening brace on same line)
- **Line length**: ~120 chars max (not enforced)
- **Constants**: `UPPER_SNAKE_CASE` as `private static final`
- **Method spacing**: Single blank line between methods

#### Annotations Order
**Class-level:**
```java
@Slf4j                          // 1. Logging
@RestController                 // 2. Spring stereotype
@RequestMapping("/api/v1/...")  // 3. Path mapping
@RequiredArgsConstructor        // 4. Constructor injection
@Validated                      // 5. Validation (if needed)
public class AuthController {
```

**Lombok patterns:**
- Controllers/Services: `@Slf4j` + `@RequiredArgsConstructor`
- Entities: `@Getter` + `@Setter` + `@NoArgsConstructor` + `@AllArgsConstructor` + `@Builder` (NEVER use `@Data` on entities)
- DTOs: `@Data` + `@NoArgsConstructor` + `@AllArgsConstructor` + `@Builder`

#### Logging (SLF4J)
- Use `@Slf4j` annotation (never create logger manually)
- Parameterized logging (never concatenate strings)
- Include context variables
```java
log.info("User registered: username={}", username);
log.warn("Login failed: {}", e.getMessage());
log.error("Database error", e);
```

#### Error Handling Pattern
**Controllers:**
```java
try {
    AuthResponse response = userService.login(request);
    log.info("Login successful for: {}", request.getUsernameOrEmail());
    return ResponseEntity.ok(response);
} catch (IllegalArgumentException e) {
    log.warn("Login failed: {}", e.getMessage());
    throw e;  // Re-throw for GlobalExceptionHandler
}
```

**Services:** Throw `IllegalArgumentException` with descriptive messages

**Global Handler:** `@RestControllerAdvice` with standardized `ErrorResponse` DTO

#### Database & Persistence
- **Repositories**: Extend `JpaRepository<Entity, ID>`, use method naming: `findByUsername`, `existsByEmail`
- **Multi-line JPQL**: Use text blocks (`"""..."""`)
- **Transactions**: `@Transactional` on write methods, `@Transactional(readOnly = true)` on reads
- **Timestamps**: Use `Instant` with `@CreationTimestamp` and `@UpdateTimestamp`
- **Collections**: Initialize with `@Builder.Default` and `new HashSet<>()`
- **Migrations**: Flyway files in `src/main/resources/db/migration/V*__description.sql`

#### Validation
```java
@NotBlank(message = "Username is required")
@Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
@Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username must contain only alphanumeric")
private String username;
```

### Frontend (Vue 3/Nuxt/TypeScript)

#### Import Organization
```typescript
import type { User, LoginRequest } from '~/types/auth'  // 1. Type imports
import { defineStore } from 'pinia'                      // 2. Library imports
import { useRuntimeConfig } from '#app'                  // 3. Nuxt/Vue imports
```

#### File Naming & Structure
- Components: PascalCase (e.g., `VideoCard.vue`, `AuthForm.vue`)
- Pages: kebab-case (e.g., `index.vue`, `video-[id].vue`)
- Composables: camelCase with `use` prefix (e.g., `useApi.ts`)
- Stores: `<name>.ts` exported as `use<Name>Store`
- Types: `types/<name>.ts` with PascalCase interfaces

#### Component Template Order
```vue
<template>
  <!-- markup -->
</template>

<script setup lang="ts">
// 1. Type/interface definitions
// 2. Props/emits definitions
// 3. Composables/stores
// 4. Reactive state (ref)
// 5. Computed properties
// 6. Functions/methods
// 7. Lifecycle hooks
// 8. Watchers
</script>

<style scoped>
/* component-specific styles */
</style>
```

#### TypeScript Patterns
- **Strict mode enabled** in `nuxt.config.ts`
- Use `interface` (not `type`) for object shapes
- Type imports: `import type { User } from '~/types/auth'`
- Props typing: `defineProps<Props>()`
- Emits typing: `defineEmits<{ update: [value: string] }>()`
- Avoid `any`; use `unknown` if type is truly unknown

#### Reactive State
```typescript
const showAdvanced = ref(false)                        // Primitives
const localFilters = ref<VideoFilters>({ ...props })  // Objects/arrays
// NOTE: Codebase uses ref() consistently, not reactive()
```

#### Stores (Pinia)
```typescript
export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    token: null,
  }),
  
  getters: {
    isLoggedIn: (state) => !!state.token,  // Arrow function syntax
  },
  
  actions: {
    async login(credentials: LoginRequest) { /* ... */ }
  },
})
```

#### Error Handling
```typescript
try {
  const response = await api.auth.login(credentials)
  return response
} catch (error) {
  console.error('Login failed:', error)
  throw error
} finally {
  // cleanup if needed
}
```

#### Naming Conventions
- Variables/functions: `camelCase` (descriptive, no abbreviations)
- Event handlers: `handle<Action>` (e.g., `handleImageError`)
- Format functions: `format<Type>` (e.g., `formatRuntime`)
- Action functions: `<verb><Noun>` (e.g., `applyFilters`, `resetFilters`)

## Testing Guidelines

**CURRENT STATUS**: Zero test coverage. When adding tests:

### Backend (JUnit 5 + Mockito)
- Test files: `src/test/java/**/*Test.java`
- `@SpringBootTest` for integration, `@WebMvcTest` for controllers, `@DataJpaTest` for repos
- Run single test: `mvn test -Dtest=UserServiceTest#testRegisterUser`
- H2 in-memory database configured in pom.xml

### Frontend (not configured yet)
- Install Vitest + Vue Test Utils
- Test files: `*.test.ts` or `*.spec.ts`

## Common Patterns

### Configuration
- Spring profiles: `dev`, `docker`, `prod`
- `@Value` for property injection, `@Bean` methods in `@Configuration` classes
- Validate required env vars at startup (see `SecurityConfigValidator`)

### Async Processing (RabbitMQ)
- Background jobs for video downloads/conversions
- Define queues with `@Bean` methods in config classes
- Consume with `@RabbitListener`

### Caching (Redis)
- Session data, rate limiting, metadata
- Set appropriate TTL (e.g., 1 month for videos)

## Critical Security Requirements

**These are MANDATORY and will be extensively checked:**
- ✅ Never store plaintext passwords (use BCrypt cost factor 12+)
- ✅ Prevent SQL injection (use JPA/parameterized queries only)
- ✅ Sanitize all user inputs (HTML/JavaScript injection prevention)
- ✅ Validate file uploads strictly (video/subtitle formats only)
- ✅ Implement rate limiting (API Gateway has tiered rate limits)
- ✅ Use HTTPS in production
- ✅ Validate JWT tokens comprehensively (signature, expiration, issuer, audience)
- ✅ Hash sensitive tokens before storage (use SHA-256)

## Known Technical Debt

**Critical Missing Features**:
1. **Torrent Integration**: `TorrentService` is PLACEHOLDER - requires libtorrent4j implementation
2. **Video Conversion**: FFmpeg integration not implemented
3. **Subtitle Download**: `SubtitleService` is PLACEHOLDER - needs OpenSubtitles API
4. **Comment Service**: Referenced in docker-compose but not built
5. **Streaming Worker**: Background processing service not implemented
6. **OAuth2 Callbacks**: Partial implementation; callback handlers incomplete
7. **Email Service**: Email sending methods exist but implementation unclear
8. **Testing**: Zero test coverage across entire codebase

## Service URLs (Local Development)

- **Eureka Dashboard**: http://localhost:8761
- **API Gateway**: http://localhost:8080
- **RabbitMQ Management**: http://localhost:15672 (user: hypertube)
- **Frontend**: http://localhost:3000
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379

## Additional Resources

- **Project Requirements**: See `CLAUDE.md` for comprehensive architecture and feature requirements
- **Security Details**: See `doc/security/SECURITY.md` for security measures and audit trail
- **Existing Agents**: See `.claude/agents/` for specialized agent configurations
