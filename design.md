# Nexa — Documento de Diseño

## 1. Visión General

**Nexa** es una plataforma de gestión educativa multi-institucion, construida como una SPA (Single Page Application) renderizada en servidor con Thymeleaf + HTMX. El frontend es reactivo sin necesidad de un framework JavaScript pesado; toda la navegación entre páginas ocurre mediante HTMX, que reemplaza solo el área de contenido principal sin recargar la página completa.

### Stack tecnológico

| Capa | Tecnología |
|---|---|
| Framework | Spring Boot 4.0.6, Spring MVC, Spring Security, Spring Data JPA |
| Templates | Thymeleaf + Layout Dialect (`nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect`) |
| Frontend interactivo | HTMX 1.9.12 + Alpine.js 3.13.8 |
| Base de datos | PostgreSQL |
| Sesiones/Caché (prod) | Redis (Spring Session + Spring Cache) |
| Build | Maven, Java 17 |
| Contenedores | Docker (app + postgres + redis) |

---

## 2. Decisiones Arquitectónicas

### 2.1 SPA sin JavaScript framework

**Decisión**: Usar Thymeleaf + HTMX en lugar de React/Vue/Angular.

**Justificación**:
- El equipo tiene experiencia en backend Java, no en SPAs JavaScript.
- HTMX permite navegación SPA (reemplazo parcial de DOM, pushState) sin salir del ecosistema Spring.
- Alpine.js cubre interactividad cliente (modales, toasts, dropdowns) con sintaxis mínima.
- Menor complejidad de build: no hay bundlers, webpack, ni node_modules en frontend.

**Consecuencia**: Cada controlador devuelve vistas Thymeleaf. Las respuestas HTMX son fragmentos (`"vista :: nombre-fragmento"`) en lugar de JSON.

### 2.2 Layout Dialect como decorador

**Decisión**: Usar `thymeleaf-layout-dialect` con patrón decorador (`layout:decorate`).

**Justificación**:
- Evita duplicar el shell HTML (sidebar, header, toast) en cada página.
- Los fragmentos `layout:fragment` permiten que cada página inyecte título, subtítulo, contenido y scripts sin sobrecarga.
- El shell (`index.html`) contiene todo el CSS/JS global, fuentes, meta CSRF y el estado Alpine.js raíz.

### 2.3 Controlador → Servicio → Repositorio (unidireccional)

**Decisión**: Capa estricta sin saltos. El Controller NUNCA llama al Repository.

**Justificación**:
- Separación de responsabilidades: Controller maneja HTTP/HTMX, Service contiene lógica de negocio y transacciones, Repository solo acceso a datos.
- Testabilidad: cada capa se prueba aislada.
- Las validaciones de negocio viven en el Service, no en el Controller.

### 2.4 DTOs solo cuando son necesarios

**Decisión**: No crear DTOs por defecto. Solo cuando hay riesgo de mass-assignment, validaciones diferenciadas crear/editar, o necesidad de aplanar relaciones.

**Justificación**:
- Para CRUD simple (ej: una tabla con 2-3 campos sin datos sensibles), el DTO añade código sin valor.
- El binding directo a Entity en el Service es más simple y legible.
- Cuando se usa DTO, el mapeo Entity ↔ DTO se hace en el Service.

### 2.5 Transacciones con rollback explícito

**Decisión**: Todo método de escritura en el Service lleva `@Transactional(rollbackFor = Exception.class)`.

**Justificación**:
- Spring solo hace rollback en `RuntimeException` por defecto. `rollbackFor = Exception.class` asegura rollback en cualquier excepción.
- Los métodos de solo lectura usan `@Transactional(readOnly = true)` para optimización de Hibernate.

### 2.6 JdbcTemplate para consultas de alto rendimiento

**Decisión**: Usar `NamedParameterJdbcTemplate` o `SimpleJdbcCall` con `RowMapper<T>` para queries de alta frecuencia (filtros, dropdowns, reportes), en lugar de JPA.

**Justificación**:
- JPA genera overhead de sesión, dirty checking y snapshots que es innecesario para consultas de solo lectura.
- Los `RowMapper<T>` garantizan type-safety sin el costo de JPA.
- Los SPs en BD permiten lógica multi-step que sería ineficiente traer a capa Java.

---

## 3. Backend

### 3.1 Estructura de paquetes

```
com.chavescr.nexa/
├── ProyectoApplication.java
├── config/           # Configuraciones Spring (@Configuration, @Profile)
├── controller/       # @Controller — endpoints HTTP + soporte HTMX
├── dto/              # DTOs (solo cuando aplica)
├── entity/           # Entidades JPA
├── exception/        # ResourceNotFoundException, BusinessException, GlobalExceptionHandler
├── repository/       # JpaRepository + JdbcTemplate (consultas rápidas)
├── security/         # SecurityConfig, CustomUserDetails, UserDetailsServiceImpl
└── service/          # @Service + @Transactional — lógica de negocio
```

### 3.2 Controllers

- Nombrados `{Entidad}Controller.java` con `@RequestMapping("/ruta")`.
- Soporte HTMX universal: cada `@GetMapping` verifica el header `HX-Request`.
  - Si es HTMX → devuelve `"carpeta/vista :: htmx-content"` (fragmento).
  - Si no → devuelve `"carpeta/vista"` (página completa decorada).
- Métodos POST/PUT/DELETE redirigen con `RedirectAttributes` + mensajes flash.
- Los mensajes flash se muestran vía el componente toast (Alpine.js) en el layout.

### 3.3 Services

- Anotados `@Service` + `@Transactional` a nivel de clase.
- `@Autowired` en campos para inyección de dependencias en Controllers. Services mantienen constructor injection.
- Lanzan `BusinessException` para violaciones de reglas, `ResourceNotFoundException` para 404.
- CRUD estándar por entidad:
  - `listarTodos()`, `obtenerPorId(Long id)`, `buscarPorNombre(String filtro)`
  - `crear(...)`, `actualizar(Long id, ...)`, `eliminar(Long id)`
  - `toggleActivo(Long id)` (soft delete)

### 3.4 Repositories

- CRUD estándar: extienden `JpaRepository<T, Long>`.
- Alto rendimiento: inyectan `JdbcTemplate` en el mismo Repository, método con `NamedParameterJdbcTemplate` + `RowMapper<T>`.
- SPs en BD: versión SQL en `src/main/resources/db/sp_*.sql`. Llamada desde Java con `SimpleJdbcCall`.

### 3.5 Domain entities

| Entity | Table | Purpose |
|---|---|---|
| Usuario | `usuarios` | Usuarios del sistema. Implementa `UserDetails`. M2M con `roles` e `instituciones`. Login por email/usuario/cédula. |
| Rol | `roles` | Roles de seguridad: `ROLE_ADMIN`, `ROLE_EDITOR`, `ROLE_USER`. |
| Institucion | `instituciones` | Centros educativos. M2M con `Usuario`. |
| Visita | `visitas` | Registro de visitantes. Ciclo de vida: PENDIENTE → AUTORIZADA/DENEGADA → FINALIZADA. |
| RegistroAsistencia | `registros_asistencia` | Asistencia de personal. Tipos: ENTRADA, SALIDA. |
| NubeNodo | `nube_nodos` | Sistema de archivos auto-referenciado. Tipos: CARPETA, ARCHIVO. |
| PeriodoAcademico | `periodos_academicos` | Períodos lectivos. UK: (institucion, codigo). |
| NivelAcademico | `niveles_academicos` | Niveles/grados/secciones. UK: (institucion, grado, seccion). |
| Materia | `materias` | Materias/asignaturas. UK: (institucion, codigo). |
| HorarioLeccion | `horario_lecciones` | Grid de horario. UK: (periodo, nivel, dia, numeroLeccion). |

### 3.6 Excepciones

- `ResourceNotFoundException`: recurso no encontrado → redirige con `errorMsg`.
- `BusinessException`: violación de regla de negocio → redirige con `errorMsg`.
- `GlobalExceptionHandler` (`@ControllerAdvice`): captura todas las excepciones y redirige con mensajes flash.

### 3.7 Perfiles Spring Boot

| Archivo | Entorno | Características |
|---|---|---|
| `application.properties` | Compartido | `spring.application.name`, `spring.profiles.active=dev`, session timeout, multipart limits |
| `application-dev.properties` | Desarrollo | `ddl-auto=update`, `thymeleaf.cache=false`, sesión en memoria, sin Redis |
| `application-prod.properties` | Producción | `ddl-auto=validate`, `thymeleaf.cache=true`, Redis (sesión + caché), Gzip, HTTP/2, connection pooling |

**Redis solo en producción**:
- Dev: `spring.session.store-type=none`, `spring.cache.type=none`
- Prod: `spring.session.store-type=redis`, `spring.cache.type=redis`, TTL 15min caché, 60min sesión

### 3.8 Configuraciones condicionales

```java
@Configuration @Profile("prod") public class RedisSessionConfig { ... }
@Configuration @Profile("prod") public class CacheConfig { ... }
```

DevTools en Docker (dev):
- `restart.enabled=false` (evita `ClassCastException`)
- `livereload.enabled=true`
- Polling cada 2s (inotify no funciona en volúmenes Docker Mac)
- Templates y recursos leídos directamente del filesystem (`file:src/main/resources/templates/`)

---

## 4. Frontend

### 4.1 Estructura de templates

```
templates/
├── index.html              # Decorador raíz (shell HTML, CSS/JS global, meta CSRF)
├── layout/
│   ├── aside.html          # Sidebar shell (toggle, logo, footer usuario)
│   ├── sidebar.html        # Nav items con HTMX (9 secciones de navegación)
│   ├── header.html         # Topbar (título, subtítulo, perfil dropdown, cambiar cuenta)
│   └── toast.html          # Toast notifications (Alpine.js)
├── auth/
│   └── login.html          # Login (standalone, sin decorador)
├── inicio/                 # Dashboard
│   ├── inicio.html
│   ├── lista-instituciones.html
│   └── instituciones-modal.html
├── {modulo}/                # Una carpeta por módulo/controlador
│   ├── index.html           # Shell: decorador + tabs + CSS global del módulo
│   ├── formulario.html      # Modal crear/editar (cuando aplica)
│   ├── tab1/                # Cada opción del menú horizontal tiene su carpeta
│   │   └── tab1.html        # Fragmento th:fragment="content" + JS propio
│   ├── tab2/
│   │   └── tab2.html
│   └── submodulo/           # Sub-módulos con menú secundario
│       ├── submodulo.html   # Fragmento con sub-tabs + JS del sub-módulo
│       ├── tabla.html       # Fragmento de tabla (recargable vía HTMX)
│       └── formulario.html  # Modal del sub-módulo
└── fragments/               # Fragmentos reusables
    ├── tabla-usuarios.html
    └── tabla-centros.html
```

**Reglas de organización:**
- Cada módulo = una carpeta en `templates/` (nombrada igual que la ruta del controlador).
- `index.html` es el shell decorado con `layout:decorate`. Contiene el CSS específico del módulo en `<style>` y el JS del módulo (funciones Alpine globales del módulo) en `layout:fragment="head-extra"`.
- Si el módulo tiene menú horizontal primario (tabs), cada opción del menú tiene su propia sub-carpeta con su HTML y su JS. El JS específico de cada tab va DENTRO del fragmento (en `<script>` o en el `x-data`), NO en el `index.html`.
- Si un tab tiene sub-tabs (menú secundario), el fragmento raíz contiene el `x-data` con las funciones CRUD y los sub-tabs. Las funciones van dentro del mismo `x-data` (ver §4.9.2).
- Tablas recargables vía HTMX van en archivos separados (ej: `tabla.html` con `th:fragment="tabla"`).
- Modales (crear/editar) van en `formulario.html` con `th:fragment="form-content"`.

### 4.2 Patrón decorador

**`index.html`** (shell):
- Define `<html th:fragment="html">` con layout:fragment slots para `title`, `head-extra`, `page-title`, `page-subtitle`, `content`.
- Incluye Google Fonts, CSS global (`style.css`, `components.css`), HTMX CDN, Alpine.js CDN.
- Meta tags CSRF + listener HTMX para adjuntar token en cada request.
- Estado Alpine.js raíz: `sidebarCollapsed`, `mobileMenuOpen`, `userMenuOpen`.
- Layout: `th:replace` de `aside`, `header`, `toast`.

**Páginas hijas**:
```html
<html layout:decorate="~{index}">
<body>
    <th:block layout:fragment="page-title">Título</th:block>
    <th:block layout:fragment="page-subtitle">Subtítulo</th:block>
    <main layout:fragment="content" th:fragment="htmx-content" x-data="miComponente()">
        ... contenido ...
    </main>
    <th:block layout:fragment="head-extra">
        <script>function miComponente() { ... }</script>
    </th:block>
</body>
</html>
```

### 4.3 Navegación SPA con HTMX

**Flujo**:
1. Usuario hace clic en link del sidebar.
2. HTMX envía `GET /ruta` con header `HX-Request: true`.
3. Controller detecta HTMX → devuelve solo `<main th:fragment="htmx-content">`.
4. HTMX inserta respuesta en `.content-area` vía `innerHTML`.
5. Navegador actualiza URL (`hx-push-url="true"`).

**CSRF**: El token se envía automáticamente vía listener `htmx:configRequest` usando los meta tags `_csrf` y `_csrf_header`.

### 4.4 Fragmentos nombrados

| Fragmento | Propósito |
|---|---|
| `htmx-content` | El `<main>` que se devuelve en respuestas HTMX parciales |
| `tabla-{entidad}` | Tabla de listado (para actualizaciones vía búsqueda) |
| `formulario` | Modal de crear/editar |
| `form-content` | Contenido del formulario modal |
| `nube-content-area` | Área de navegación de archivos en Nube Nexa |
| `modal-content` | Contenido de diálogo modal genérico |

### 4.5 Búsqueda en tiempo real (Alpine.js + HTMX)

```html
<main x-data="listaFiltrable()">
    <input x-model="filtro" @input.debounce.300ms="buscar()">
    <th:block th:replace="~{dominio/index :: tabla-dominio}" />
</main>
<script>
function listaFiltrable() {
    return {
        filtro: '',
        buscar() {
            const params = new URLSearchParams({ filtro: this.filtro });
            htmx.ajax('GET', '/dominio/buscar?' + params.toString(), {
                target: '#tabla-dominio', swap: 'outerHTML'
            });
        }
    };
}
</script>
```

### 4.6 Modales (HTMX + Alpine.js)

- Botón disparador: `hx-get="/ruta/formulario"` + `hx-target="#modal-container"` + `@click="abrirModal = true"`.
- Contenedor: `x-show="abrirModal"` + listener `@close-modal.window` para cerrar.
- Formulario (en `formulario.html`): emite `$dispatch('close-modal')` al cancelar/guardar.

### 4.7 Toast notifications (Alpine.js)

- Componente en `layout/toast.html`. Escucha evento `@notify.window`.
- El backend envía `HX-Trigger` header con `{"notify": {"type": "success", "message": "..."}}`.
- Se oculta automáticamente tras 3 segundos.

### 4.8 CSS y estilos

- CSS global en `static/css/style.css` y `static/css/components.css`.
- Variables CSS en `:root` dentro del decorador `index.html`.
- CSS específico por página: archivo en `static/css/` cargado con `<link>` en `layout:fragment="head-extra"`.
- Clases utilitarias: `d-flex`, `gap-{1-4}`, `text-muted`, `text-sm`, `font-medium`, etc.

### 4.9 Configuración de menús horizontales (tabs y sub-tabs)

El proyecto usa dos niveles de menú horizontal con HTMX + Alpine.js. Ambos siguen el mismo mecanismo: Alpine.js maneja el estado visual (`activeTab`/`subTab`) y HTMX carga el contenido bajo demanda.

#### 4.9.1 Menú horizontal primario (Nivel 1 — Tabs de página)

Es el menú de pestañas principal dentro del `<main>` de una página decorada. Se usa en `personal/index.html`, `agenda/index.html` y `configuracion-academica/index.html`.

**Estructura obligatoria:**

```html
<!-- 1. CONTENEDOR Alpine con estado activeTab -->
<div x-data="{ activeTab: 'tab1' }">

    <!-- 2. CONTENEDOR CSS de tabs -->
    <div class="config-tabs-container">
        <div class="config-tabs">

            <!-- 3. CADA BOTÓN sigue este patrón exacto -->
            <button class="config-tab-btn"
                    :class="{ 'active': activeTab === 'tab1' }"
                    @click="activeTab = 'tab1'"
                    hx-get="/modulo/tab1"
                    hx-target="#modulo-content"
                    hx-swap="innerHTML">
                🏷️ Nombre Tab
            </button>
            <!-- ... más tabs ... -->
        </div>
    </div>

    <!-- 4. CONTENEDOR de contenido HTMX -->
    <div id="modulo-content" class="tab-pane"
         hx-get="/modulo/tab1"
         hx-trigger="load"
         hx-swap="innerHTML">
    </div>
</div>
```

**CSS requerido (copiar tal cual):**
```css
.config-tabs-container {
    margin-bottom: 2rem;
    background: #ffffff;
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 6px;
    display: flex;
    width: 100%;
    box-shadow: var(--shadow-sm);
    overflow-x: auto;
}
.config-tabs {
    display: flex;
    width: 100%;
    gap: 6px;
}
.config-tab-btn {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 10px 16px;
    font-size: 0.85rem;
    font-weight: 600;
    color: var(--text-secondary);
    text-decoration: none;
    border-radius: 10px;
    transition: all 0.25s ease;
    cursor: pointer;
    border: none;
    background: transparent;
    white-space: nowrap;
}
.config-tab-btn:hover {
    color: var(--primary);
    background-color: var(--input-bg);
}
.config-tab-btn.active {
    color: white !important;
    background-color: var(--primary) !important;
    box-shadow: 0 4px 14px rgba(45, 90, 135, 0.25);
}
.tab-pane {
    animation: fadeIn 0.35s ease-out;
}
@keyframes fadeIn {
    from { opacity: 0; transform: translateY(6px); }
    to { opacity: 1; transform: translateY(0); }
}
```

**Reglas del menú primario:**
- El `activeTab` default es el primer tab.
- El `hx-trigger="load"` en el contenedor carga el contenido del primer tab al abrir la página.
- Cada tab tiene su propio endpoint en el controller que devuelve `"carpeta/archivo :: content"`.
- El `hx-target` es siempre el `id` del contenedor de contenido.
- El `hx-swap` es siempre `"innerHTML"`.
- Alpine.js solo controla la clase `.active` visual; HTMX controla la carga del contenido.

#### 4.9.2 Menú horizontal secundario (Nivel 2 — Sub-tabs dentro de un fragmento)

Es un sub-menú que aparece dentro de un fragmento cargado por un tab primario. El caso de referencia es `personal/regimen/regimen.html`. Se usa cuando un tab primario necesita segregar contenido en sub-categorías.

**Estructura obligatoria:**

```html
<!-- 1. El fragmento raíz contiene x-data con subTab + funciones -->
<div th:fragment="content" class="tab-pane"
     x-data="{ subTab: 'TODOS', modalOpen: false,
         abrirForm(id) { ... },
         eliminar(id) { ... }
     }">

    <!-- 2. Sub-tabs con Alpine + HTMX -->
    <div class="reg-tabs">
        <button class="reg-tab"
                :class="{ 'active': subTab === 'TODOS' }"
                @click="subTab = 'TODOS'"
                hx-get="/modulo/submodulo?tipo=TODOS"
                hx-target="#sub-content"
                hx-swap="innerHTML">📋 Todos</button>
        <button class="reg-tab"
                :class="{ 'active': subTab === 'TIPO_A' }"
                @click="subTab = 'TIPO_A'"
                hx-get="/modulo/submodulo?tipo=TIPO_A"
                hx-target="#sub-content"
                hx-swap="innerHTML">🟡 Tipo A</button>
        <!-- ... más sub-tabs ... -->
    </div>

    <!-- 3. Contenedor del contenido de sub-tabs -->
    <div id="sub-content" th:replace="~{modulo/submodulo/tabla :: tabla}"></div>

    <!-- 4. Modal container compartido -->
    <div id="modal-container" x-show="modalOpen" x-cloak style="display: none;"
         @close-modal.window="modalOpen = false"
         @keydown.escape.window="modalOpen = false"></div>
</div>
```

**CSS para sub-tabs (copiar tal cual, ajustar prefijo):**
```css
.reg-tabs {
    display: flex;
    gap: 6px;
    margin-bottom: 1.25rem;
    background: #fff;
    border: 1px solid var(--border);
    border-radius: 10px;
    padding: 4px;
}
.reg-tab {
    flex: 1;
    text-align: center;
    padding: 8px 12px;
    font-size: 0.82rem;
    font-weight: 600;
    color: var(--text-secondary);
    border-radius: 7px;
    cursor: pointer;
    border: none;
    background: transparent;
    transition: all 0.2s;
    white-space: nowrap;
}
.reg-tab:hover {
    color: var(--primary);
    background: var(--input-bg);
}
.reg-tab.active {
    color: #fff;
    background: var(--primary);
}
```

**Reglas del menú secundario:**
- El `subTab` default es `'TODOS'` (o el primer valor lógico).
- Las funciones CRUD (`abrirForm`, `eliminar`, `cambiarEstado`) van DENTRO del mismo `x-data` del fragmento, NO en `<script>` separado ni en `Alpine.data()`.
- El contenido inicial se carga con `th:replace` (renderizado servidor en la primera carga).
- Los sub-tabs subsecuentes cargan vía HTMX (`hx-get` con query param `?tipo=`).
- El modal container usa `x-show="modalOpen"` y se controla con `@close-modal.window`.
- El `hx-target` del sub-tab apunta al contenedor `#sub-content`.
- La tabla fragment (`tabla.html`) lleva `th:fragment="tabla"`.

**⚠️ Error común a evitar:** NO anidar dos `x-data` en el fragmento secundario. Si el `x-data` está en el `th:fragment`, los sub-tabs deben estar directamente dentro (sin otro `x-data` intermedio), porque un `x-data` anidado rompe el acceso a las funciones y propiedades del scope padre.

---

## 5. Seguridad

### 5.1 Autenticación

- Spring Security con `UserDetailsServiceImpl` personalizado.
- Login por email, usuario o cédula (campo único `identifier`).
- `Usuario` implementa `UserDetails`.
- Página de login en `auth/login.html` (sin decorador, standalone).

### 5.2 Autorización

- `ROLE_ADMIN`: acceso total, incluyendo gestión de instituciones y usuarios.
- `ROLE_EDITOR`: gestión académica y operativa.
- `ROLE_USER`: acceso básico.
- La sección "Administración" del sidebar solo visible con `ROLE_ADMIN`.

### 5.3 CSRF

- `CookieCsrfTokenRepository.withHttpOnlyFalse()` para evitar crear sesión al renderizar login.
- Meta tags `_csrf` y `_csrf_header` en el layout.
- Listener HTMX `htmx:configRequest` adjunta el token en cada petición.

### 5.4 Sesión

- Dev: sesión en memoria (sin Redis).
- Prod: sesión en Redis (`spring-session-data-redis`).
- Timeout: 60 minutos.
- Cookie: `SESSION`, HttpOnly, SameSite=Lax.

---

## 6. Nube Nexa (Sistema de archivos)

### 6.1 Modelo auto-referenciado

`NubeNodo` se referencia a sí mismo: `padre` (M2O) y `hijos` (O2M). Tipos: `CARPETA`, `ARCHIVO`.

### 6.2 Operaciones

| Operación | Descripción |
|---|---|
| Crear carpeta | Crea `NubeNodo` con `tipo=CARPETA`. No crea directorio físico. |
| Subir archivo | Guarda `MultipartFile` en filesystem (`${ruta.recursos}`). Crea `NubeNodo` con metadatos (tamaño, extensión). |
| Descargar archivo | Streaming con `UrlResource`. |
| Descargar carpeta | ZIP streaming recursivo. |
| Previsualizar | Content-Disposition inline. Detecta tipo (PDF, imagen, texto, video, audio). |
| Renombrar | Actualiza `nombre` en BD y archivo físico. |
| Eliminar | Borrado recursivo: archivos físicos + registros BD. Transaccional. |

### 6.3 Breadcrumb

`obtenerRutaBreadcrumb(Long nodoId)` recorre `padre` recursivamente hasta la raíz para construir la ruta de navegación.

---

## 7. Control de Acceso (Visitas)

### 7.1 Ciclo de vida

```
PENDIENTE → AUTORIZADA → (registra ingreso) → (registra salida) → FINALIZADA
         ↘ DENEGADA (fin)
```

### 7.2 Funcionalidades

- **Registrar visita**: `POST /control-de-acceso/registrar` → estado PENDIENTE.
- **Autorizar/Denegar**: `POST /control-de-acceso/autorizar` o `/denegar` → transición de estado.
- **Registrar ingreso/salida**: timestamps `fechaHoraIngreso` / `fechaHoraSalida`.
- **Búsqueda**: filtro por nombre, identificación, estado, fecha.
- **Asistencia de personal**: `RegistroAsistencia` ENTRADA/SALIDA con contador de presentes.

---

## 8. Configuración Académica

Módulo CRUD completo para la estructura académica de cada institución:

| Sub-módulo | Entidad | Operaciones |
|---|---|---|
| Períodos | `PeriodoAcademico` | CRUD + toggle activo |
| Niveles | `NivelAcademico` | CRUD + toggle activo. UK: (grado, seccion) |
| Materias | `Materia` | CRUD + toggle activo. Campo `color` para identificación visual |
| Horario | `HorarioLeccion` | Grid semanal (L-V) × 8 lecciones. Asigna materia + docente a cada celda |

Todas las operaciones requieren `SESSION_INSTITUCION_ID` de la sesión. Cada entidad valida pertenencia a la institución antes de operar.

---

## 9. Convenciones

### 9.1 Nombres de archivos Java

- `Entidad.java` (singular, PascalCase)
- `EntidadController.java`, `EntidadService.java`, `EntidadRepository.java`
- `EntidadDTO.java` (solo si aplica)

### 9.2 Nombres de templates

- `{entidad}/index.html` — vista principal
- `{entidad}/formulario.html` — modal crear/editar
- `{entidad}/subfuncion/index.html` — sub-sección

### 9.3 Rutas HTTP

- `GET /entidad` — vista principal
- `GET /entidad/formulario?id=` — formulario (crear/editar)
- `POST /entidad` — crear
- `PUT /entidad/{id}` — actualizar
- `DELETE /entidad/{id}` — eliminar
- `PUT /entidad/{id}/toggle` — activar/desactivar
- `GET /entidad/buscar?filtro=` — búsqueda (devuelve fragmento tabla)

### 9.4 Sesiones HTTP

Toda clave en `HttpSession` sigue el formato:

```
SESSION_<OBJETO>_<PROPIEDAD>
```

Ejemplos:
```java
session.setAttribute("SESSION_INSTITUCION_ID", id);
session.setAttribute("SESSION_INSTITUCION_NOMBRE", nombre);
session.setAttribute("SESSION_USUARIO_ID", usuarioId);
session.getAttribute("SESSION_USUARIO_ROL");
```

**Las claves del `Model` NO llevan este prefijo.** Solo aplica a `HttpSession`.

### 9.5 Injection

**Controllers**: usar `@Autowired` en campos. No usar constructor injection.

```java
@Controller
@RequestMapping("/modulo")
public class ModuloController {

    @Autowired
    private MiService service;

    // métodos...
}
```

**Services**: constructor injection con `private final`. Sin `@Autowired` en campos.

```java
@Service
@Transactional
public class MiService {

    private final MiRepository repository;
    private final OtroService otroService;

    public MiService(MiRepository repository, OtroService otroService) {
        this.repository = repository;
        this.otroService = otroService;
    }
}
```

### 9.6 Logging

- `log.info(...)` para operaciones de escritura exitosas (registrar ID y entidad).
- `log.error(...)` para errores en catch blocks.
- Usar `@Slf4j` de Lombok o `LoggerFactory.getLogger()`.

---

## 10. Docker

### 10.1 Servicios

| Servicio | Puerto | Perfil |
|---|---|---|
| `app` | 8080 | Dev: hot-reload, debug remoto 5005. Prod: multi-stage, non-root user |
| `db` | 5432 | PostgreSQL |
| `redis` | 6379 | Solo prod |

### 10.2 Dockerfiles

- `Dockerfile`: multi-stage build (Maven + JDK → JRE runtime).
- `Dockerfile.dev`: hot-reload con DevTools + volumen del código fuente + remote debug.
- `Dockerfile.prod`: multi-stage optimizado, non-root user, healthcheck.

### 10.3 Volúmenes (dev)

```yaml
volumes:
  - .:/app                    # Código fuente para hot-reload
  - maven_cache:/root/.m2     # Cache de dependencias Maven
```

---

## 11. Checklist para nuevos módulos

Al agregar un nuevo dominio (ej: "Curso"):

1. **Entity**: `entity/Curso.java` con anotaciones JPA.
2. **Repository**:
   - CRUD estándar: extender `JpaRepository<Curso, Long>`.
   - Alto rendimiento: inyectar `JdbcTemplate`, agregar `RowMapper<T>` y método con `NamedParameterJdbcTemplate` o `SimpleJdbcCall`.
   - Si usa SP: crear script SQL en `src/main/resources/db/sp_NombreProcedimiento.sql`.
3. **DTO**: `dto/CursoDTO.java` con validaciones Jakarta. **Solo si aplica** (formularios con campos sensibles, validaciones diferenciadas crear/editar, o necesidad de aplanar relaciones).
4. **Service**: `service/CursoService.java` con `@Transactional` y CRUD completo.
   - Cache Redis: `@Cacheable` / `@CacheEvict` (solo prod).
5. **Controller**: `controller/CursoController.java` con `@RequestMapping("/curso")` y soporte HTMX.
   - Datos en sesión con prefijo `SESSION_`.
6. **Templates**: `templates/curso/index.html` (decorador + tabla + Alpine.js + modal container) y `templates/curso/formulario.html` (modal).
7. **Sidebar**: Agregar link en `layout/sidebar.html` con HTMX.
8. **Verificar**: `mvn compile`.
