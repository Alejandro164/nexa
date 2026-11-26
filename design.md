# Nexa — Documento de Diseño

Nexa (Núcleo de Experiencia Académica) es la plataforma de gestión de un centro educativo costarricense. Este documento describe el sistema tal como está en el código. `AGENTS.md` recoge las reglas cortas que hay que cumplir al editar. Si un párrafo de aquí y el código discrepan, manda el código y se corrige este archivo.

## 1. Visión

La interfaz es una SPA de servidor: Thymeleaf pinta el HTML y HTMX sustituye `.content-area` sin recargar el shell (sidebar, topbar, toast). Alpine.js guarda el estado de la pantalla (pestaña activa, modal abierto, filtros). No hay bundler ni `node_modules`.

Los textos de la interfaz y los mensajes de validación están en español.

### Stack

| Capa | Tecnología |
|---|---|
| Framework | Spring Boot 4.0.6, Spring MVC, Spring Security, Spring Data JPA |
| Templates | Thymeleaf + Layout Dialect (`nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect`) + `thymeleaf-extras-springsecurity6` |
| Interacción | HTMX 1.9.12 + Alpine.js 3.13.8 (CDN, en `templates/index.html`) |
| Base de datos | PostgreSQL 15 |
| Sesión y caché en prod | Redis 7 (`spring-session-data-redis`) |
| Correo en dev | Mailpit |
| Build | Maven wrapper, Java 17 |
| Contenedores | `docker-compose.yml` (dev), `docker-compose.prod.yml` (prod) |

## 2. Decisiones que el código sigue

### SPA con Thymeleaf + HTMX

Cada pantalla es una vista Thymeleaf. HTMX pide el mismo `GET` con el header `HX-Request: true` y el controller devuelve un fragmento. El sidebar usa `hx-get`, `hx-target=".content-area"`, `hx-swap="innerHTML"` y `hx-push-url="true"`.

### Layout Dialect

`templates/index.html` es el shell: CSS global, HTMX, Alpine, meta CSRF, sidebar, topbar y toast. Las páginas hijas declaran `layout:decorate="~{index}"` e inyectan `page-title`, `page-subtitle`, `content` y, cuando hace falta, `head-extra`.

### Controller → Service → Repository

El controller habla HTTP y HTMX. El service tiene la regla de negocio y la transacción. El repository es una interfaz Spring Data. Un controller nuevo no inyecta un repository. Hay controllers antiguos que sí lo hacen; ese atajo no se extiende.

### DTO solo cuando aporta

Un formulario simple se enlaza a la entidad. Se crea un DTO de escritura cuando hay mass-assignment, validaciones distintas entre crear y editar, o hay que aplanar relaciones. El mapeo vive en el service.

Para pintar una vista que junta varias tablas se usa un DTO de lectura (`dto/Fila*`, `Vista*`, `Panel*`). Eso no obliga a crear un DTO de escritura.

### Transacciones con rollback explícito

Spring solo revierte `RuntimeException`. Por eso la escritura lleva `@Transactional(rollbackFor = Exception.class)` y la lectura `@Transactional(readOnly = true, rollbackFor = Exception.class)`.

### Consultas

Los repository son `JpaRepository` (u otra interfaz Spring Data) con métodos derivados o `@Query`. `JdbcTemplate` aparece en seeders y en `DatabaseIndexInitializer`, no en los repository de dominio. No hay procedimientos almacenados ni carpeta `src/main/resources/db/`.

## 3. Tenencia

Una `Institucion` es el centro (cédula jurídica, nombre). Una `Direccion` es una sede u oferta educativa de esa institución (`oferta`, `principal`, código). El usuario tiene muchas direcciones y una dirección tiene muchos usuarios.

La sesión trabaja con una dirección a la vez:

| Clave | Contenido |
|---|---|
| `SESSION_DIRECCION_ID` | Dirección activa. Es el filtro de tenant de casi todas las consultas. |
| `SESSION_DIRECCION_NOMBRE` | Nombre que muestra el shell. |
| `SESSION_USUARIO_ID` | Id del usuario autenticado. |
| `SESSION_PUEDE_CAMBIAR_INSTITUCION` | El shell lo recuerda para no repetir la consulta en cada fragmento HTMX. |
| `SESSION_GESTION_MATERIA_ID` | Materia elegida en Gestión Académica. |

Los atributos del `Model` no usan el prefijo `SESSION_`.

`SesionDireccionService.resolver` corre en el login y en `GET /`. Si hay una sola dirección, la selecciona. Si hay varias, intenta la última elegida. `ROLE_ADMIN` puede seguir sin dirección (solo Inicio y Administración). `ROLE_SYSTEM_CONFIG` lista todas las instituciones; el resto, solo las suyas. Si un controller necesita dirección y no está en sesión, lanza `DireccionNoSeleccionadaException`.

Toda consulta de datos escolares filtra por `SESSION_DIRECCION_ID` y, antes de actualizar o borrar, comprueba que la fila pertenece a esa dirección.

## 4. Roles y autorización

Roles en `Rol.nombre` y en `DataInitializer`:

| Rol | Qué abre |
|---|---|
| `ROLE_SYSTEM_CONFIG` | Configuración de plataforma, usuarios, seguridad, direcciones, preferencias de cuenta. Ve todas las instituciones. |
| `ROLE_ADMIN` | Operación de la dirección y los módulos marcados solo para administración. Puede entrar sin dirección elegida. |
| `ROLE_DIRECTOR` | Operación de su dirección, incluida la configuración académica e institucional. |
| `ROLE_DOCENTE` | Aula: gestión académica de sus materias, conducta, notas, classroom, contacto. |
| `ROLE_ESTUDIANTE` | Consulta: horario, classroom, rendimiento, comunicación, nube, comedor, impresiones. |
| `ROLE_PADRE` | Lo mismo que el estudiante en consulta, más reuniones. |

La matriz exacta está en `SecurityConfig.filterChain`. Reglas para tocarla:

- El authority ya trae el prefijo `ROLE_`. Se usa `hasAuthority` / `hasAnyAuthority`, nunca `hasRole`.
- El primer `requestMatchers` que coincide gana. Una ruta nueva va **antes** de `.anyRequest().authenticated()`. Si no, cualquier sesión la abre.
- El ítem del sidebar repite el mismo corte con `sec:authorize` y con `x-show="ve('admin director docente estudiante padre')"`. `ve()` lee el modo “Ver el menú como” de `layout/aside.html` (admin, system, director, docente, estudiante, padre, auxiliar, guarda). `sec:authorize` lee el rol real. Hacen falta los dos: uno decide si el HTML existe y el otro si el modo de vista lo muestra.
- El alcance fino (un docente solo ve sus materias, un padre solo ve a sus hijos) vive en el service, no en `SecurityConfig`. Ejemplo: `MenuController.docenteIdSiAplica`, `HorarioConsultaService`, `ClassroomService`.

El principal inyectado con `@AuthenticationPrincipal` es `CustomUserDetails`. `UserDetailsServiceImpl` carga el `Usuario` por email, usuario o cédula y lo envuelve. El parámetro del formulario de login se llama `email`.

CSRF es el repositorio de sesión. No hay `CookieCsrfTokenRepository`. El shell publica `_csrf` y `_csrf_header`, y `htmx:configRequest` copia el token a cada request HTMX.

Sesión: 60 minutos. En dev, en memoria. En prod, Redis, cookie `SESSION`, HttpOnly, SameSite Lax (`RedisSessionConfig`). Una sesión expirada durante un request HTMX responde 200 con `HX-Redirect` a `/login?expired`, para que navegue el browser entero. Un 403 HTMX redirige igual a `/inicio`.

## 5. Backend

```
com.chavescr.nexa/
├── ProyectoApplication.java
├── config/        # seeders @Profile("dev"), CacheConfig, RedisSessionConfig, WebMvcConfig, jobs
├── controller/    # @Controller
├── dto/           # modelos de vista y, cuando aplica, de escritura
├── entity/        # una entidad por tabla
├── exception/     # DireccionNoSeleccionadaException, GlobalExceptionHandler, correo y WhatsApp
├── repository/    # interfaces Spring Data
├── security/      # SecurityConfig, CustomUserDetails, UserDetailsServiceImpl, login handlers
└── service/       # un paquete plano
```

No hay subpaquetes de service ni de controller. Un módulo nuevo no crea un paquete nuevo: crea las clases en esos paquetes y la carpeta de templates.

### Controllers

- `@Controller` y `@RequestMapping("/ruta")`. Inyección con `@Autowired` en campos. `MenuController` usa constructor; los nuevos siguen el `@Autowired` de campos.
- El shell de varios módulos está en `MenuController`. Las acciones de ese módulo están en otro controller con el mismo prefijo de URL (`/gestion-academica` en `MenuController`, `/gestion-academica/asistencia` en `AsistenciaController`). Hay que buscar el prefijo completo antes de agregar un método.
- Página del módulo: si `HX-Request` es `"true"`, `"carpeta/index :: htmx-content"`; si no, `"carpeta/index"`.
- Tab, lista o modal: siempre `"carpeta/archivo :: content"` o `":: form-content"`, sin mirar `HX-Request`.
- Una mutación HTMX devuelve el fragmento actualizado. El mensaje al usuario va en `HX-Trigger`.
- Un POST de página completa (control de acceso) sí redirige con `RedirectAttributes` y `successMsg` / `errorMsg`. Esos atributos los pinta la propia vista, no el toast global.
- `@ResponseBody` queda para streaming de archivos y para las búsquedas JSON que ya existen en Nube, visitas y estudiantes.

`GlobalModelAttributesAdvice` llena notificaciones, `sinDireccionAdmin`, el menú de direcciones y si el asistente está disponible. En un request HTMX no vuelve a consultar el menú: el shell no se redibuja.

### Services

`@Service`. Constructor con `private final`. Escritura y lectura con el `@Transactional` de la sección 2. El mensaje de una regla rota es `IllegalArgumentException` en español; el controller lo muestra.

### Excepciones

| Clase | Uso |
|---|---|
| `DireccionNoSeleccionadaException` | Falta `SESSION_DIRECCION_ID`. `GlobalExceptionHandler` responde redirect a `/`, con `HX-Redirect` si es HTMX. |
| `CorreoEnvioException`, `WhatsAppApiException` | Fallos de esos canales. Las atrapa el controller del módulo. |
| `IllegalArgumentException` | Regla de negocio. |

`GlobalExceptionHandler` no convierte el resto de las excepciones en flash messages.

### Perfiles

| Archivo | Qué fija |
|---|---|
| `application.properties` | Nombre, `spring.profiles.active=dev`, sesión 60m, multipart 100MB. OSIV sigue activo: las vistas recorren lazy loads. No se desactiva. |
| `application-dev.properties` | `ddl-auto=update`, SQL en log, sesión y caché en memoria, Thymeleaf y estáticos desde `file:src/main/resources/`, DevTools con restart y LiveReload (polling 2s), `ruta.recursos`, SMTP a Mailpit, clave de WhatsApp de desarrollo. |
| `application-prod.properties` | `ddl-auto=validate`, Thymeleaf con caché, Redis, gzip, `ruta.recursos`, SMTP y `WHATSAPP_ENCRYPTION_KEY` por entorno. |

`CacheConfig` (TTL 15 min) y `RedisSessionConfig` solo existen con `@Profile("prod")`.

## 6. Frontend

### Árbol

```
templates/
├── index.html                 # shell
├── layout/                    # aside, sidebar, header, toast, notificaciones
├── auth/login.html            # sin decorador
├── fragments/                 # trozos reutilizables (preview de archivo, tabla de instituciones)
├── {modulo}/
│   ├── index.html             # página decorada: th:block htmx-content + main
│   ├── formulario.html        # modal, th:fragment="form-content", cuando aplica
│   └── {tab}/
│       └── {tab}.html         # th:fragment="content"
└── componentes/               # catálogo visual en /componentes (ROLE_ADMIN)
```

La carpeta repite la ruta del controller: `/personal/regimen/todos` → `templates/personal/regimen/todos/todos.html`. Un sub-módulo repite la forma del módulo. Referencia real: `templates/personal/regimen/`.

```
personal/regimen/
├── index.html                 # th:fragment="content", x-data, sub-tabs, modal
├── formulario.html
├── todos/todos.html           # th:fragment="content"
├── llamadas/llamadas.html
├── amonestaciones/amonestaciones.html
└── procesos/procesos.html
```

`index.html` de un sub-módulo cargado como tab no lleva `layout:decorate`. Solo el `index.html` que abre el sidebar es página decorada.

### Página decorada

```html
<html layout:decorate="~{index}">
<body>
    <h2 layout:fragment="page-title">Título</h2>
    <p layout:fragment="page-subtitle">Subtítulo</p>

    <th:block th:fragment="htmx-content">
        <h2 id="topbar-page-title" hx-swap-oob="true">Título</h2>
        <p id="topbar-page-subtitle" hx-swap-oob="true">Subtítulo</p>
        <main layout:fragment="content">
            ...
        </main>
    </th:block>
</body>
</html>
```

El `th:block` es el fragmento HTMX, no el `<main>`. Los dos nodos con `hx-swap-oob="true"` actualizan el topbar, que está fuera de `.content-area`. Sin ellos, al cambiar de módulo el título se queda en el anterior.

### Tabs

Nivel 1, dentro del `<main>` decorado (`personal/index.html`, `agenda/index.html`, `configuracion-academica/index.html`):

- `x-data="{ activeTab: '...' }"`.
- Botones `.config-tab-btn` con `:class`, `@click`, `hx-get`, `hx-target="#...-content"`, `hx-swap="innerHTML"`.
- El panel tiene `hx-get` del primer tab y `hx-trigger="load"`, o un `th:replace` del primer fragmento.
- Alpine solo cambia la clase `.active`. HTMX trae el HTML.
- El CSS de `.config-tabs-container`, `.config-tabs`, `.config-tab-btn` y `.tab-pane` se copia en un `<style>` de esa página. Ya está en los módulos citados.

Nivel 2, dentro del fragmento (`personal/regimen/index.html`):

- El `x-data` del fragmento guarda `subTab`, `modalOpen` y las funciones (`abrirForm`, `eliminar`, `cambiarEstado`).
- Esas funciones no van a un `<script>` suelto ni a `Alpine.data()`.
- El primer panel entra con `th:replace`. Los siguientes, con `hx-get` al endpoint del sub-tab.
- No se anida otro `x-data`: el interno no ve las funciones del padre.

### Modales

El botón pide `hx-get="/ruta/form"` hacia `#modal-container` y abre el flag de Alpine (solo si `event.detail.successful`). El contenedor escucha `@close-modal.window` y Escape. El fragmento del formulario es `form-content`. Al guardar, el controller devuelve la lista actualizada o, si hay error de validación, reescribe el modal con `HX-Retarget` y `HX-Reswap` (comprobar `!event.detail.xhr.getResponseHeader('HX-Retarget')` antes de cerrar el modal).

Estilo obligatorio de modales: **minimalistas**, sin fondos de color ni elementos visuales innecesarios. Superficie blanca (`#ffffff`), borde fino (`#e2e8f0`), divisores sutiles, tipografía limpia y botones sobrios (evitar franjas rojas superiores, cajas con relleno de color o íconos decorativos llamativos). Referencia: `templates/estudiantes/components/confirmar-desactivar.html` y `templates/estudiantes/expedientes/ficha.html`.

### Avisos

`layout/toast.html` escucha `@notify.window` y se oculta a los 3 segundos. Para usarlo, el `HX-Trigger` dispara `notify` con `{type, message}`.

La mayoría de los módulos disparan un evento propio (`academicoGuardado`, `usuarioError`, `classroomGuardado`, …) y la pantalla lo escucha. Al tocar un módulo, se reutiliza el evento que esa pantalla ya escucha.

### CSS y JavaScript

- Global: `static/css/style.css` y `static/css/componentes.css`, más las variables de `:root` en el shell.
- De una pantalla: un `<style>` dentro de su HTML. El shell, en `htmx:afterSwap`, vuelve a insertar esos `<style>` porque Chrome no aplica los que llegan dentro del HTML de HTMX.
- No se agrega un CSS por página en `static/css/`.
- JS de un tab: dentro del fragmento, en `<script>` o en el `x-data`.
- JS del módulo decorado que Alpine necesita en la carga completa: `layout:fragment="head-extra"`.
- `static/js/componentes.js` se carga con `defer` antes de Alpine. Ahí viven funciones globales que el primer pintado ya referencia (`nubeNexaComponente`, `planTrabajoComponente`). Solo se añade algo ahí cuando el shell lo necesita antes de Alpine.
- `static/js/configuracion/**` es legado de esas pantallas. Se edita el archivo existente; no se crea otro.
- `const` por defecto, `let` si la variable se reasigna. Las funciones que sobreviven a un swap se declaran `function nombre() {}`.

`templates/index.html` deja `htmx.config.defaultSettleDelay = 0` y monta Alpine después de ejecutar los scripts del swap. También destruye el árbol Alpine del HTML que sale. Una vista cuya información cambia en otra pantalla lleva `hx-history="false"`; al volver con Atrás el shell recarga la página (`refreshOnHistoryMiss`).

El catálogo visual está en `/componentes` (`templates/componentes/`). Las clases utilitarias (`d-flex`, `gap-*`, `text-muted`, `text-sm`, `font-medium`) salen de `style.css`.

## 7. Esquema

En dev Hibernate actualiza el esquema (`ddl-auto=update`). En prod solo valida (`ddl-auto=validate`).

No hay Flyway ni Liquibase. Un cambio de tabla o columna lleva un SQL idempotente en `docker/migraciones/AAAA-MM-DD-descripcion.sql`. La cabecera de los archivos existentes dice qué cambia, de qué script depende, que en dev lo crea Hibernate y que en prod hay que aplicarlo con `psql` antes de desplegar. `docker/init/01-seed.sql` solo corre con el volumen de Postgres vacío.

`DataInitializer` (`@Profile("dev")`, `@Order(10)`) siembra roles, instituciones, direcciones, usuarios y estructura académica. Es idempotente. También corren otros seeders de dev (`CicloEducativoSeeder`, `TipoMateriaSeeder`, `TipoAulaSeeder`, `ComponenteEvaluacionSeeder`).

## 8. Nube Nexa

`NubeNodo` es un árbol (`padre` / `hijos`) de tipo `CARPETA` o `ARCHIVO`. La carpeta no crea directorio físico. El archivo se guarda bajo `${ruta.recursos}` (`/home/datos-nexa` en dev y en prod). En Docker ese path es el volumen `datos_nexa_dev` o `datos_nexa_prod`.

Operaciones en `NubeNodoService` y `NubeController` (`/nube-nexa`): crear carpeta, subir, descargar archivo, descargar carpeta en ZIP, previsualizar, renombrar, borrar en cascada, compartir. El breadcrumb recorre `padre` hasta la raíz. La cédula jurídica de la institución participa en la ruta física.

## 9. Cómo se corre

### Dev (`docker compose up`)

| Servicio | Publicación | Notas |
|---|---|---|
| `app` | `localhost:8082` → 8080, debug `5005` | `Dockerfile.dev`, código montado en `/app`, Maven en el volumen `maven_cache` |
| `db` | `localhost:5433` → 5432 | Postgres 15, base `nexa`, usuario `postgres`, clave `1234` |
| `mailpit` | UI `8025`, SMTP `1025` | El contenedor de la app usa `MAIL_HOST=mailpit` |
| `redis` | `6379` | Solo con el perfil de Compose `prod` |

Un `./mvnw` lanzado en el host usa `localhost:5433`. El proceso de dentro del contenedor recibe `SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/nexa`.

Cuentas de dev (las crea `DataInitializer`):

| Usuario | Clave | Rol |
|---|---|---|
| `admin` | `admin` | ADMIN y SYSTEM_CONFIG, las tres direcciones |
| `director` | `123` | DIRECTOR, Liceo Alpha |
| `carlos.lopez` | `user1234` | DOCENTE |
| `rosa.jimenez` | `user1234` | PADRE |
| `sofia.alpizar` | `user1234` | ESTUDIANTE |

### Prod (`docker-compose.prod.yml`)

La app se construye con `Dockerfile` (no hay `Dockerfile.prod`). Publica `${APP_PORT:-8082}:8080`. Postgres está fuera de Compose (`host.docker.internal`). Redis exige clave. El volumen de archivos es `datos_nexa_prod`.

## 10. Mapa de módulos

La columna “quién” resume `SecurityConfig`. El matcher exacto manda sobre esta tabla. “Shell” es quién responde el `GET` de la página.

| Ruta | Templates | Shell | Quién entra |
|---|---|---|---|
| `/`, `/inicio` | `inicio/` | `MainController` | Autenticado |
| `/contacto` | `contacto/` | `ContactoController` | Admin, director, docente |
| `/estudiantes` | `estudiantes/` | `MenuController` + `EstudiantesController` | Lectura: staff. Escritura: admin, director |
| `/docentes` | `docentes/` | `DocentesController` | Admin, director, docente |
| `/personal` | `personal/` | `MenuController` + `PersonalController` | Admin, director. Solicitudes: también docente |
| `/control-de-acceso` | `control-acceso/` | `VisitaController` | Admin, director |
| `/gestion-academica` | `gestion-academica/` | `MenuController` + asistencia, promedio, historial, componente | Admin, director, docente. Un docente ve sus materias |
| `/rendimiento-academico` | `rendimiento-academico/` | `RendimientoAcademicoController` | Admin, estudiante, padre |
| `/notas` | `notas/` | `MenuController` + `NotasController` | Admin, director, docente |
| `/conducta` | `conducta/` | `ConductaController` | Admin, director, docente |
| `/classroom` | `classroom/` | `ClassroomController` | GET: también padre. Escritura: admin, director, docente, estudiante. La clase se valida en el service |
| `/horario` | `horario/` | `HorarioController` | Admin, director, docente, estudiante, padre |
| `/horario-periferica` | `horario-periferica/` | `HorarioPerifericaController` | Admin, director, docente |
| `/entregas-academicas` | `entregas-academicas/` | `EntregasAcademicasController` | Staff. Mantenimiento: admin, director |
| `/agenda` | `agenda/` | `MenuController` + `AgendaController` + `TareaController` | Calendario: todos. Reuniones: staff y padre. Actividad: admin, director. Resto: staff |
| `/comunicacion` | `comunicacion/` | `ComunicacionController` | Todos los roles |
| `/comedor` | `comedor/` | `MenuController` | Todos los roles de la comunidad |
| `/impresiones` | `impresiones/` | `ImpresionesController` | Comunidad. La cola de imprimir: admin, director |
| `/nube-nexa` | `nube/` | `NubeController` | GET: staff, estudiante, padre. Escritura: staff |
| `/oficios` | `oficios/` | `OficioController` | Admin, director |
| `/apelaciones`, `/ampliacion` | `apelaciones/`, `ampliacion/` | `MenuController` | Admin, director, docente |
| `/coordinacion-academica` | `coordinacion-academica/` | `MenuController` | Admin, director. El sidebar lo muestra solo a admin |
| `/archivo-graduados` | `archivo/` | `MenuController` | Admin, director |
| `/archivos` | sin vista: descarga de archivo | `ArchivoController` | Admin, director, docente |
| `/reportes` | `reportes/` | `MenuController` | Admin, director, docente |
| `/mis-cursos` | `curso/` | `CursoController` | Admin, director, docente |
| `/gestion-especial`, `/evaluacion-academica` | su carpeta | `MenuController` | Admin, director, docente. El sidebar los muestra solo a admin |
| `/configuracion-academica`, `/configuracion-institucional`, `/grupos-por-area` | su carpeta | su controller | Admin, director |
| `/configuracion`, `/usuarios`, `/seguridad`, `/direcciones` | su carpeta | su controller | SYSTEM_CONFIG |
| `/configuracion-cuenta` | `configuracion-cuenta/` | `ConfiguracionCuentaController` | SYSTEM_CONFIG |
| `/perfil` | `perfil/` | `PerfilController` | Autenticado |
| `/componentes` | `componentes/` | `ComponentesController` | Admin |
| `/login` | `auth/login.html` | `AuthController` | Público |

`/asistente` es el widget de chat del shell. Su disponibilidad la decide `AsistenteConfiguracionService` y solo se evalúa en la página completa.

## 11. Checklist de un módulo nuevo

1. Entidad en `entity/`, si hay tabla nueva. Repository Spring Data en `repository/`.
2. SQL idempotente en `docker/migraciones/` para prod. En dev, Hibernate aplica el cambio al arrancar.
3. Service con constructor, `@Transactional(rollbackFor = Exception.class)` en la escritura y `readOnly` en la lectura. Filtra por `SESSION_DIRECCION_ID`. Si falta, `DireccionNoSeleccionadaException`.
4. DTO solo en los casos de la sección 2.
5. Controller `@Controller` con `@Autowired`. El `GET` de la página distingue `HX-Request`. Tabs y modales devuelven `:: content` o `:: form-content`.
6. `requestMatchers` en `SecurityConfig` antes de `anyRequest`, con `hasAnyAuthority` y el rol completo.
7. Templates como en la sección 6, copiando una página del mismo tipo (`personal/index.html` si tiene tabs). Título OOB incluido.
8. Ítem en `layout/sidebar.html` con `sec:authorize` y `x-show="ve(...)"`, más `hx-push-url="true"`.
9. `./mvnw compile`. No ejecutar pruebas automatizadas (`./mvnw test`) salvo que el usuario las solicite explícitamente.

## 12. Pruebas

**Regla para agentes**: no ejecutar `./mvnw test` ni correr pruebas automatizadas por iniciativa propia; solo hacerlo cuando el usuario lo pida explícitamente. Para validar cambios alcanza con `./mvnw compile`.

Los tests de service usan `@ExtendWith(MockitoExtension.class)` y no levantan Spring ni Postgres.

`ProyectoApplicationTests`, `EntregasAcademicasFlujoTest` y `TareaDetalleFlujoTest` son `@SpringBootTest`. Necesitan el Postgres de dev en `localhost:5433`. En ese arranque corre `DataInitializer` contra esa base. `./mvnw test` ejecuta ambos grupos. El plugin hace fork de la JVM de test.
