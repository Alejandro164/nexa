# AGENTS.md — Nexa

High-signal facts for agents working in this repo. Every line answers: "Would an agent miss this without help?"

## Build & Run

```bash
mvn compile          # Verify compilation
mvn test             # Run tests (Spring context, needs dev DB)
docker compose up    # Dev env: app on :8082, DB on :5433, debug on :5005
```

- **Spring Boot 4.0.6** (not 3.x). Java 17. Maven wrapper (`./mvnw`).
- The `spring-boot-maven-plugin` has `<fork>true</fork>` — tests fork a separate JVM.
- LiveReload is enabled in dev. Polling interval is 2s (inotify doesn't work in Docker volumes on Mac).

## Architecture: Controller → Service → Repository (strict)

- Controller NEVER calls Repository directly. Service is the only layer that touches the DB.
- Controller returns Thymeleaf views (never JSON). Uses `@Controller`, not `@RestController`.
- `@GetMapping` methods check `HX-Request` header: if `"true"` → return fragment `"vista :: htmx-content"`. Otherwise return the full decorated page.
- POST/PUT/DELETE methods redirect with `RedirectAttributes.addFlashAttribute()` for toast messages.

## Templates & HTMX

- **Decorator pattern**: `templates/index.html` is the shell. All child pages use `layout:decorate="~{index}"`.
- The `<main>` in every child page must carry BOTH `layout:fragment="content"` AND `th:fragment="htmx-content"`. This is what enables HTMX partial rendering.
- **Fragment naming**: `htmx-content` for the main area, `tabla-{entity}` for tables, `formulario` or `form-content` for modals.
- Alpine.js state in each page goes in `layout:fragment="head-extra"` as a `<script>`.
- CSRF token is sent automatically via `htmx:configRequest` listener in `index.html`. Uses meta tags `_csrf` and `_csrf_header`.
- Session expiry with HTMX: `SecurityConfig.redirectToLogin()` sends `HX-Redirect` header so the full browser follows it, not just the HTMX fragment.
- **Template directories mirror controller names**: controller `CursoController` with `@RequestMapping("/curso")` → templates go in `templates/curso/`.
- **Module structure**: `index.html` (shell + decorator + module CSS/JS), `formulario.html` (modal). Each horizontal tab gets its own subfolder with its HTML fragment and JS. Sub-modules with secondary menus get their own subfolder with fragments (main, table, form). JS goes inline in the fragment or in `head-extra`, NEVER in a separate `.js` file outside the templates folder.

## Database & Entities

- All entities are in `com.chavescr.nexa.entity.*`. One entity per table.
- Multi-tenancy is per-institution. Institutions are stored in `SESSION_INSTITUCION_ID` (HttpSession).
- Session key convention: `SESSION_<OBJECT>_<PROPERTY>` (e.g., `SESSION_INSTITUCION_ID`). Model attributes do NOT use this prefix.
- `DataInitializer` (`@Profile("dev")`) seeds 3 roles, 3 institutions, 5 users on startup (idempotent). Useful for dev but could interfere with tests.

## ruta.recursos (file storage)

- **Critical**: `ruta.recursos=/home/datos-nexa` is defined only in `application-dev.properties`. NubeNodo storage will crash without it in other profiles.
- In Docker dev, `/home/datos-nexa` is mounted from host path `/Users/alejandrochaves/datos-nexa` (see `docker-compose.yml` volumes).

## Code conventions (must follow when editing)

- **Controllers**: `@Autowired` on fields. Services: constructor injection preferred. Some old services (`NubeNodoService`) still use `@Autowired` on fields — do NOT replicate that pattern in new services, but it's fine for controllers.
- Services: `@Service` + `@Transactional` at class level. Write methods: `@Transactional(rollbackFor = Exception.class)`. Read methods: `@Transactional(readOnly = true, rollbackFor = Exception.class)`.
- DTOs only when needed: mass-assignment risk, different create/edit validations, or need to flatten relationships. Otherwise bind directly to entity.
- Template directories mirror controller names: controller `CursoController` with `@RequestMapping("/curso")` → templates go in `templates/curso/`.
- No `GlobalExceptionHandler` exists yet. Services throw `IllegalArgumentException` directly. The `exception/` package in the design doc is aspirational.

## Security

- Login accepts email, username, or cédula (field name is `email` in the form).
- `CookieCsrfTokenRepository` is NOT yet configured. The default `HttpSessionCsrfTokenRepository` is in use. If adding public pages, switch to the cookie-based one or you'll get `IllegalStateException`.
- `DaoAuthenticationProvider` is constructed explicitly with the `UserDetailsService` as a constructor arg. Spring Boot 4.x changed the API from setter to constructor.

## Tests

- `ProyectoApplicationTests` tests Thymeleaf template rendering against real `SpringTemplateEngine` + HTMX session-expiry redirect with `MockMvc`.
- Tests need the `application-dev.properties` database configured. Run with Docker dev DB up, or tests will fail on context load.

## Perfiles

- `application.properties` → shared config, sets `spring.profiles.active=dev`.
- `application-dev.properties` → DB at `localhost:5432`, ddl-auto=update, no Redis, Thymeleaf cache off, file-system template loading.
- `application-prod.properties` → DB at `db:5432`, ddl-auto=validate, Thymeleaf cache on, Redis for sessions + cache, Gzip on, HTTP/2 on.
- Redis config classes (`CacheConfig`, `RedisSessionConfig`) only activate with `@Profile("prod")`.
