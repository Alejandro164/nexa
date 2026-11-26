# AGENTS.md — Nexa

Hechos que un agente no puede deducir solo con leer un archivo. El mapa de módulos, el modelo de tenencia y el checklist de un módulo nuevo están en `design.md`. Léelo antes de agregar una pantalla o una ruta.

## Build y entorno

```bash
./mvnw compile       # compila
./mvnw test          # toda la suite; los @SpringBootTest necesitan Postgres
docker compose up    # app http://localhost:8082  debug :5005  Mailpit http://localhost:8025
```

- Spring Boot **4.0.6**, Java 17. El wrapper es `./mvnw`.
- `spring-boot-maven-plugin` tiene `<fork>true</fork>`: los tests corren en otra JVM.
- Desde el host, `application-dev.properties` apunta a `localhost:5433` (usuario `postgres`, clave `1234`, base `nexa`). Dentro del contenedor `app` la URL se sobreescribe a `jdbc:postgresql://db:5432/nexa`.
- DevTools tiene restart y LiveReload activos. El polling es de 2s porque inotify no cruza el volumen de Docker en Mac. En dev, Thymeleaf y los estáticos se leen de `file:src/main/resources/`.
- Mailpit captura el correo de dev (SMTP 1025, UI 8025). No sale a internet.
- Redis no levanta con `docker compose up`. Solo entra con el perfil de Compose `prod` o con `docker-compose.prod.yml`.
- Cuentas que siembra `DataInitializer` (`@Profile("dev")`, idempotente): `admin` / `admin` (ADMIN + SYSTEM_CONFIG, las tres direcciones), `director` / `123` (DIRECTOR, Liceo Alpha), `carlos.lopez` / `user1234` (DOCENTE), `rosa.jimenez` / `user1234` (PADRE), `sofia.alpizar` / `user1234` (ESTUDIANTE). El campo del login se llama `email` y acepta email, usuario o cédula.

## Capas

- Controller → Service → Repository. Un controller nuevo no inyecta un repository. Varios controllers viejos sí lo hacen (`UsuariosController`, `EstudiantesController`, `NubeController`, `VisitaController`, `DocentesController`, `ContactoController`): no copies ese atajo y no lo “arregles” si no es parte del cambio.
- Controllers: `@Autowired` en campos. Services nuevos: constructor e inyección con `private final`. Quedan services viejos con `@Autowired` en campos (`VisitaService`, `RegistroAsistenciaService`, `RetiroEstudianteService`, `SolicitudService`).
- Services: `@Service`. Escritura `@Transactional(rollbackFor = Exception.class)`. Lectura `@Transactional(readOnly = true, rollbackFor = Exception.class)`.
- Un controller devuelve una vista Thymeleaf. `@ResponseBody` solo para descarga o streaming de archivos y para búsquedas puntuales que ya lo usan (Nube, visitas, estudiantes). No abras una API JSON para una pantalla.
- DTO de escritura solo si hay mass-assignment, validaciones distintas de crear y editar, o hay que aplanar relaciones. Los DTO de lectura (`Fila*`, `Vista*`, `Panel*`) sí se usan para lo que pinta la vista.
- Errores de negocio: `IllegalArgumentException` con el mensaje en español. El controller lo convierte en fragmento con error o en `HX-Trigger`. No existen `BusinessException` ni `ResourceNotFoundException`.
- `GlobalExceptionHandler` solo atrapa `DireccionNoSeleccionadaException` y manda a `/` (`HX-Redirect` si el request es HTMX). No redirige el resto de las excepciones.

## Tenencia

- `Institucion` es el centro educativo. `Direccion` es una sede u oferta dentro de esa institución. El usuario se asigna a direcciones (M2M).
- La clave de tenant es `SESSION_DIRECCION_ID`. Toda lectura y escritura de datos escolares filtra por esa dirección y comprueba que la fila le pertenece antes de modificarla.
- Si la sesión no tiene dirección, lanza `DireccionNoSeleccionadaException`. No redirijas a mano.
- Quien resuelve la sesión es `SesionDireccionService` (login y `GET /`). No inventes otra forma de elegir dirección.
- `ROLE_SYSTEM_CONFIG` ve todas las instituciones. `ROLE_ADMIN` puede entrar sin dirección: el menú queda en Inicio + Administración (`sinDireccionAdmin`).
- Otras claves de sesión: `SESSION_DIRECCION_NOMBRE`, `SESSION_USUARIO_ID`, `SESSION_PUEDE_CAMBIAR_INSTITUCION`, `SESSION_GESTION_MATERIA_ID`. Los atributos del `Model` no llevan prefijo `SESSION_`.
- El principal de Spring Security es `CustomUserDetails`, no `Usuario`. `Usuario` también implementa `UserDetails`, pero el login devuelve el wrapper.

## Seguridad

- Roles reales: `ROLE_ADMIN`, `ROLE_DIRECTOR`, `ROLE_DOCENTE`, `ROLE_PADRE`, `ROLE_ESTUDIANTE`, `ROLE_SYSTEM_CONFIG`.
- Usa `hasAuthority` / `hasAnyAuthority`. `hasRole` antepondría `ROLE_` por segunda vez.
- Cada ruta nueva necesita un `requestMatchers` en `SecurityConfig` **antes** de `.anyRequest().authenticated()`. El primer matcher gana. Sin matcher explícito, cualquier usuario autenticado entra.
- La misma entrada del menú lleva `sec:authorize` y `x-show="ve('admin director …')"` en `layout/sidebar.html`. `ve()` mira el modo “Ver el menú como” de `layout/aside.html`; `sec:authorize` mira el rol real. Si falta uno de los dos, el ítem se ve en un sitio y no en el otro.
- CSRF es el de sesión (`HttpSessionCsrfTokenRepository`). No está `CookieCsrfTokenRepository`. El token viaja por los meta `_csrf` y `_csrf_header` y el listener `htmx:configRequest` de `templates/index.html`.
- Sesión expirada o 401 con HTMX: `SecurityConfig.redirectToLogin()` responde 200 + `HX-Redirect`. Un 403 HTMX hace `HX-Redirect` a `/inicio`. No devuelvas un 401/403 pelado en un fragmento.
- `DaoAuthenticationProvider` recibe el `UserDetailsService` por constructor (Spring Security 6 / Boot 4).

## Pantallas HTMX

Hay dos niveles de respuesta. Confundirlos rompe el shell.

1. **Página del módulo** (la que abre el sidebar). El `GET` mira `HX-Request`: si es `"true"` devuelve `"carpeta/index :: htmx-content"`; si no, `"carpeta/index"`. El fragmento es un `<th:block th:fragment="htmx-content">` que envuelve el `<main layout:fragment="content">` **y** estos dos nodos, para que el topbar (fuera de `.content-area`) cambie de título:

   ```html
   <h2 id="topbar-page-title" hx-swap-oob="true">Título</h2>
   <p id="topbar-page-subtitle" hx-swap-oob="true">Subtítulo</p>
   ```

   `layout:fragment="page-title"` y `page-subtitle` siguen haciendo falta para la carga completa. Copia la estructura de `templates/personal/index.html`.

2. **Tab, tabla o modal**. El endpoint devuelve siempre `"carpeta/archivo :: content"` o `":: form-content"`. No mira `HX-Request`: nadie pide esa URL como página completa. El archivo no lleva `layout:decorate`.

Varios shells viven en `MenuController` (`/personal`, `/agenda`, `/gestion-academica`, `/notas`, …) y las acciones en otro controller con el mismo prefijo (`AsistenciaController`, `PromedioController`, `ConductaController`). Busca todos los `@RequestMapping` del prefijo antes de agregar un endpoint.

Mutaciones HTMX (el caso normal): el controller devuelve el fragmento ya actualizado (la lista o la tabla). El aviso al usuario es un header `HX-Trigger` con un evento que esa pantalla escucha. El toast global (`layout/toast.html`) solo escucha `notify` con `{type, message}`. `successMsg` / `errorMsg` por flash solo los pinta control de acceso y el login; un redirect con flash no aparece solo en el toast.

Si el formulario debe quedarse abierto con el error, responde con `HX-Retarget` y `HX-Reswap` hacia el contenedor del modal (véase `PersonalController.directorioGuardar`) o con 422 para que HTMX no sustituya el contenido (véase `ClassroomController`).

Tabs horizontales: Alpine guarda `activeTab` y la clase `.active`; HTMX carga el panel (`hx-target` + `hx-swap="innerHTML"`). El panel inicial usa `hx-trigger="load"` o un `th:replace` del primer fragmento. El CSS de `.config-tabs*` se copia dentro de un `<style>` del `index.html` del módulo. En un sub-módulo el estado y las funciones CRUD van en el mismo `x-data` del fragmento (`personal/regimen/index.html`). Un `x-data` anidado deja de ver las funciones del padre.

`templates/index.html` ya resuelve dos problemas de HTMX. No los reimplementes:

- Un `<style>` dentro de un fragmento no lo aplica Chrome. `afterSwap` lo reinyecta. El CSS de una pantalla va en un `<style>` del propio HTML.
- Alpine se monta después de los `<script>` del swap (`defaultSettleDelay = 0` y `montarAlpine`). Una función que la página completa necesita al primer pintado (`x-data="nombre()"` en el shell) va en `layout:fragment="head-extra"` o, si es de un componente global ya cargado antes de Alpine, en `static/js/componentes.js`. El JS de un tab va dentro de su fragmento.

JavaScript: nada de `var` nuevo. `const` por defecto, `let` si se reasigna. Las funciones que tienen que seguir vivas entre swaps se declaran `function nombre() {}`. No crees un `.js` por módulo. `static/js/configuracion/**` es legado: si tocas esa pantalla, edita ese archivo. `static/js/componentes.js` es global y carga antes de Alpine.

Vistas cuyo contenido cambia en otra pantalla llevan `hx-history="false"`. Al volver con Atrás, el shell recarga la página completa (`refreshOnHistoryMiss`).

Textos de interfaz, validaciones y mensajes de error van en español.

## Esquema

- Dev: `ddl-auto=update`. Hibernate altera las tablas al arrancar.
- Prod: `ddl-auto=validate`. Un campo nuevo sin SQL previo impide el arranque.
- No hay Flyway ni Liquibase. Cada cambio de esquema agrega un SQL idempotente en `docker/migraciones/AAAA-MM-DD-descripcion.sql`, con el comentario de cabecera de los archivos que ya están (orden, qué columnas, que en dev lo crea Hibernate y en prod hay que aplicarlo antes del deploy).
- `docker/init/01-seed.sql` solo corre cuando el volumen de Postgres está vacío.
- `spring.jpa.open-in-view` sigue activo a propósito. No lo apagues: las vistas navegan asociaciones lazy.

## Archivos (Nube Nexa)

- `ruta.recursos=/home/datos-nexa` está en dev y en prod. Sin esa propiedad el almacenamiento falla.
- En Docker es un volumen nombrado (`datos_nexa_dev` / `datos_nexa_prod`), no un bind al home de nadie.

## Perfiles

- `application.properties`: común, `spring.profiles.active=dev`, sesión 60m, multipart 100MB.
- `application-dev.properties`: `ddl-auto=update`, sin Redis, Thymeleaf sin caché y leído del filesystem, SMTP hacia Mailpit.
- `application-prod.properties`: `ddl-auto=validate`, Thymeleaf con caché, Redis, gzip. `CacheConfig` y `RedisSessionConfig` son `@Profile("prod")`. La cookie de sesión en prod se llama `SESSION`. `WHATSAPP_ENCRYPTION_KEY` es obligatoria en prod.

## Tests

- La mayoría de `src/test` son Mockito (`@ExtendWith(MockitoExtension.class)`) y no necesitan base.
- `ProyectoApplicationTests`, `EntregasAcademicasFlujoTest` y `TareaDetalleFlujoTest` son `@SpringBootTest`: cargan el contexto dev y Postgres en `localhost:5433`. `DataInitializer` corre en ese arranque y escribe en esa base.
- `./mvnw test` ejecuta las dos clases. Si Postgres no está arriba, fallan los de contexto, no los de Mockito.
