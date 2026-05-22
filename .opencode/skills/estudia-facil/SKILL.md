---
name: estudia-facil
description: Use ONLY when working on the EstudiaFácil project (Spring Boot + Thymeleaf + HTMX + Alpine.js). Covers backend architecture (Controller → Service → Repository), transactional CRUD with rollback, SPA-like navigation with HTMX fragments, template structure mirroring controllers, Alpine.js components per page, DTO validation, exception handling, CSRF for HTMX, and the Thymeleaf Layout Dialect decorator pattern. Project root: src/main/java/com/empresa/proyecto/
---

# EstudiaFácil — Arquitectura y Convenciones

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Framework | Spring Boot, Spring MVC, Spring Security, Spring Data JPA |
| Templates | Thymeleaf + Layout Dialect (`nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect`) |
| Frontend interactivo | HTMX 1.9.12 + Alpine.js 3.13.8 |
| Validación | Jakarta Validation (`spring-boot-starter-validation`) |
| Base de datos | PostgreSQL (dev: `localhost:5432/project_db`) |
| Build | Maven, Java 17 |

Dependencias esenciales en `pom.xml`:
```xml
<dependency>
    <groupId>nz.net.ultraq.thymeleaf</groupId>
    <artifactId>thymeleaf-layout-dialect</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

## 1. Arquitectura Backend

La jerarquía es estrictamente unidireccional:

```
Controller  →  Service  →  Repository  →  Entity
     ↓            ↓
   (DTO)     Exception     ← DTO solo cuando hay seguridad/formularios complejos
```

**Reglas:**
- El Controller NUNCA llama directamente al Repository.
- El Controller puede bindear formularios a DTOs **cuando se requiera seguridad** (mass-assignment, over-posting) o validaciones específicas distintas entre crear/editar. Para formularios simples sin riesgo, se puede bindear directo a la Entity en el Service.
- El Service contiene la lógica de negocio y es el único que usa el Repository.
- El Repository solo extiende `JpaRepository`, sin lógica adicional.
- Las Entities son POJOs con anotaciones JPA. Implementan `UserDetails` si son el usuario del sistema.

### 1.1 Paquete `controller/`

- Un controlador por cada dominio/entidad principal.
- Usa `@Controller` (nunca `@RestController`) porque devuelve vistas Thymeleaf.
- Métodos `@GetMapping` para vistas completas, `@PostMapping`/`@PutMapping`/`@DeleteMapping` para operaciones CRUD.
- Soporte HTMX: cada método GET verifica el header `HX-Request`. Si es `"true"`, devuelve solo el fragmento (`"vista :: nombre-fragmento"`). Si no, devuelve la página decorada completa.

```java
@GetMapping
public String index(Model model, HttpServletRequest request) {
    model.addAttribute("items", service.listarTodos());
    if ("true".equals(request.getHeader("HX-Request"))) {
        return "dominio/index :: htmx-content";
    }
    return "dominio/index";
}
```

- Métodos POST/PUT redirigen con `RedirectAttributes` y `addFlashAttribute` para mensajes toast.
- Métodos DELETE redirigen igual.

### 1.2 Paquete `service/`

- Anotado `@Service` + `@Transactional` a nivel de clase.
- **Todo método de escritura** lleva `@Transactional(rollbackFor = Exception.class)`.
- Métodos de solo lectura: `@Transactional(readOnly = true, rollbackFor = Exception.class)`.
- Si una operación viola reglas de negocio, lanza `BusinessException`.
- Si un recurso no existe, lanza `ResourceNotFoundException`.
- El servicio usa `private final` + constructor injection (sin `@Autowired`).

```java
@Service
@Transactional
public class DominioService {

    private final DominioRepository repository;

    public DominioService(DominioRepository repository) {
        this.repository = repository;
    }

    @Transactional(rollbackFor = Exception.class)
    public Dominio crear(DominioDTO dto) {
        validarReglaNegocio(dto);
        Dominio entity = new Dominio();
        mapearDtoAEntidad(dto, entity);
        Dominio guardado = repository.save(entity);
        log.info("Dominio creado: id={}", guardado.getId());
        return guardado;
    }

    @Transactional(rollbackFor = Exception.class)
    public void eliminar(Long id) {
        Dominio entity = obtenerPorId(id);
        if (!entity.getRelaciones().isEmpty()) {
            throw new BusinessException("No se puede eliminar: tiene dependencias.");
        }
        repository.delete(entity);
    }
}
```

### 1.3 Paquete `dto/` (solo cuando corresponda)

**Cuándo crear un DTO:**
- Formularios que exponen campos sensibles (roles, permisos, flags de estado como `activo`).
- Validaciones distintas entre crear y editar (ej: contraseña obligatoria al crear, opcional al editar).
- Relaciones lazy que causarían `LazyInitializationException` en la vista.
- Necesidad de aplanar/proyectar datos específicos que no están en una sola entidad.

**Cuándo NO crear un DTO:**
- CRUD simple de 2-3 campos sin campos sensibles (ej: una tabla `categorias` con `id` y `nombre`).
- La vista solo muestra/edita todos los campos de la entidad sin restricciones especiales.

Cuando se usa DTO:
- Es un POJO con getters/setters, sin lógica.
- Usa anotaciones Jakarta Validation: `@NotBlank`, `@Size`, `@Email`.
- El mapeo Entity ↔ DTO se hace en el Service.

Cuando NO se usa DTO:
- El Controller pasa/recibe directamente la Entity al Service.
- Las validaciones se hacen en el Service con `BusinessException`.

```java
// Sin DTO — formulario simple que bindea directo
@PostMapping
public String crear(@RequestParam String nombre, RedirectAttributes redirect) {
    service.crear(nombre);
    redirect.addFlashAttribute("successMsg", "Creado exitosamente.");
    return "redirect:/categoria";
}
```

### 1.4 Paquete `exception/`

- `ResourceNotFoundException`: recurso no encontrado (404).
- `BusinessException`: violación de regla de negocio (400).
- `GlobalExceptionHandler`: clase con `@ControllerAdvice` que captura todas las excepciones y redirige con mensajes flash.

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("errorMsg", ex.getMessage());
        return "redirect:/";
    }
}
```

## 2. Arquitectura Frontend (Templates)

### 2.1 Estructura de directorios

Los templates reflejan la estructura de controladores:

```
templates/
├── index.html                    (solo si es necesario, payload mínimo decorador)
├── layout/
│   ├── base.html                 (shell HTML: <head>, CSS, JS, layout:fragment slots)
│   ├── aside.html                (sidebar estructura: toggle, logo, footer usuario)
│   ├── sidebar.html              (nav items con HTMX)
│   ├── header.html               (topbar con layout:fragment para título/subtítulo)
│   └── toast.html                (notificaciones Alpine.js)
├── dominio/                      (carpeta x controlador)
│   ├── index.html                (vista principal: listado, búsqueda, modal container)
│   ├── formulario.html           (modal crear/editar)
│   └── subseccion/               (sub-secciones del dominio)
│       └── index.html
└── auth/
    └── login.html
```

**Regla**: cada controlador = una carpeta en `templates/`. Si el controlador tiene sub-rutas, crea sub-carpetas.

### 2.2 Patrón layout:decorate

`layout/base.html` define el shell HTML con `th:fragment="html"`. Las páginas hijas usan `layout:decorate="~{layout/base}"`.

**base.html** (`th:fragment="html"`):
```html
<html th:fragment="html" xmlns:layout="...">
<head>
    <!-- TODO el CSS, JS, fuentes, meta CSRF -->
    <title layout:fragment="title">Default</title>
    <th:block layout:fragment="head-extra"></th:block>
</head>
<body x-data="{ sidebarCollapsed: false, mobileMenuOpen: false, userMenuOpen: false }">
    <div class="sidebar-overlay" ...></div>
    <th:block th:replace="~{layout/aside :: aside}" />
    <div class="main-wrapper">
        <th:block th:replace="~{layout/header :: header}" />
        <main class="content-area" layout:fragment="content"></main>
    </div>
    <th:block th:replace="~{layout/toast :: toast}" />
</body>
</html>
```

**Página hija** (`layout:decorate="~{layout/base}"`):
```html
<html layout:decorate="~{layout/base}">
<body>
    <th:block layout:fragment="page-title">Título</th:block>
    <th:block layout:fragment="page-subtitle">Subtítulo</th:block>
    <main layout:fragment="content" th:fragment="htmx-content" x-data="componenteAlpine()">
        <!-- contenido -->
    </main>
    <th:block layout:fragment="head-extra">
        <script>function componenteAlpine() { return { /* estado */ }; }</script>
    </th:block>
</body>
</html>
```

### 2.3 Fragmentos Thymeleaf nombrados

Cada fragmento reusable se define con `th:fragment="nombre"` y se incluye con `th:replace="~{ruta :: nombre}"`:

```html
<!-- definición en empresa/formulario.html -->
<div th:fragment="formulario" class="modal-backdrop">...</div>

<!-- uso en otra vista -->
<div th:replace="~{empresa/formulario :: formulario}"></div>
```

**Convención de nombres de fragmentos:**
- `htmx-content`: el `<main>` que se devuelve en respuestas HTMX parciales.
- `tabla-{entidad}`: la tabla de listado (para actualizaciones parciales vía búsqueda).
- `formulario`: el modal de crear/editar.

## 3. Patrón SPA con HTMX

### 3.1 Links del sidebar

Cada link de navegación en `layout/sidebar.html` usa atributos HTMX:

```html
<a href="/empresa" class="nav-item"
   hx-get="/empresa"
   hx-target=".content-area"
   hx-swap="innerHTML"
   hx-push-url="true">
```

### 3.2 Flujo completo

1. Usuario hace clic en link del sidebar.
2. HTMX envía `GET /empresa` con header `HX-Request: true`.
3. Controller detecta `HX-Request`, devuelve `"empresa/index :: htmx-content"`.
4. Thymeleaf renderiza solo el `<main th:fragment="htmx-content">`.
5. HTMX inserta la respuesta en `.content-area` con `innerHTML`.
6. El navegador actualiza la URL (`hx-push-url="true"`).

### 3.3 CSRF

`layout/base.html` incluye meta tags y un listener HTMX:

```html
<meta name="_csrf" th:content="${_csrf.token}" />
<meta name="_csrf_header" th:content="${_csrf.headerName}" />
<script>
    document.addEventListener('htmx:configRequest', function(evt) {
        var token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
        var header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
        if (token) evt.detail.headers[header] = token;
    });
</script>
```

### 3.4 Búsqueda en tiempo real con Alpine.js + HTMX

Patrón para búsqueda con debounce:

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
                target: '#tabla-dominio',
                swap: 'outerHTML'
            });
        }
    };
}
</script>
```

El controller tiene un endpoint `/buscar` que devuelve solo el fragmento de la tabla:
```java
@GetMapping("/buscar")
public String buscar(@RequestParam String filtro, Model model) {
    model.addAttribute("items", service.buscarPorNombre(filtro));
    return "dominio/index :: tabla-dominio";
}
```

### 3.5 Modales con HTMX + Alpine.js

```html
<!-- Botón que abre el modal -->
<button hx-get="/dominio/formulario"
        hx-target="#modal-container"
        hx-swap="innerHTML"
        @click="abrirModal = true">
    Nuevo
</button>

<!-- Contenedor del modal -->
<div id="modal-container"
     x-show="abrirModal"
     @close-modal.window="abrirModal = false"
     @keydown.escape.window="abrirModal = false"
     x-cloak style="display:none;">
</div>
```

El formulario (`formulario.html`) emite evento para cerrar:
```html
<div th:fragment="formulario" class="modal-backdrop"
     @click.self="$dispatch('close-modal')">
    <button @click="$dispatch('close-modal')">Cancelar</button>
</div>
```

## 4. CRUD transaccional completo

Para cada entidad, implementar estas operaciones en el Service:

| Método | Anotación | Descripción |
|---|---|---|
| `listarTodos()` | `@Transactional(readOnly=true, rollbackFor=Exception.class)` | SELECT * |
| `obtenerPorId(Long id)` | `@Transactional(readOnly=true, rollbackFor=Exception.class)` | SELECT by id, lanza `ResourceNotFoundException` si no existe |
| `buscarPorNombre(String filtro)` | `@Transactional(readOnly=true, rollbackFor=Exception.class)` | Búsqueda con filtro LIKE |
| `crear(DTO dto)` | `@Transactional(rollbackFor=Exception.class)` | INSERT con validaciones de negocio |
| `actualizar(Long id, DTO dto)` | `@Transactional(rollbackFor=Exception.class)` | UPDATE con validaciones |
| `eliminar(Long id)` | `@Transactional(rollbackFor=Exception.class)` | DELETE con verificación de dependencias |
| `toggleActivo(Long id)` | `@Transactional(rollbackFor=Exception.class)` | Activar/desactivar |

Cada operación de escritura registra un `log.info(...)` con el resultado.

## 5. Validaciones

- DTOs anotados con Jakarta Validation.
- Controller usa `@Valid @ModelAttribute` y recibe `BindingResult`.
- Si hay errores de validación, se devuelve el fragmento del formulario para mostrar errores inline.
- `GlobalExceptionHandler` captura `MethodArgumentNotValidException` y redirige con mensaje.

## 6. Convenciones de nombres

### Archivos Java
- `Entidad.java` (singular, PascalCase)
- `EntidadController.java`
- `EntidadService.java`
- `EntidadRepository.java`
- `EntidadDTO.java`

### Templates
- `entidad/index.html` — vista principal (listado)
- `entidad/formulario.html` — formulario crear/editar (modal)
- `entidad/subfuncion/index.html` — sub-sección

### Rutas
- `GET /entidad` — vista principal
- `GET /entidad/formulario?id=` — formulario (crear o editar)
- `POST /entidad` — crear
- `PUT /entidad/{id}` — actualizar
- `DELETE /entidad/{id}` — eliminar
- `PUT /entidad/{id}/toggle` — activar/desactivar
- `GET /entidad/buscar?filtro=` — búsqueda partial (devuelve fragmento tabla)

## 7. CSS y estilos

- CSS global en `<style>` dentro de `layout/base.html`.
- CSS específico por página: crear archivo `.css` en `static/css/` y cargar con `<link>` en el `layout:fragment="head-extra"`.
- Variables CSS en `:root` dentro de base.html.
- Clases utilitarias: `d-flex`, `gap-{1-4}`, `text-muted`, `text-sm`, `font-medium`, etc.

## 8. Crear un nuevo módulo — checklist

Para agregar un nuevo dominio (ej: "Curso"):

1. **Entity**: `entity/Curso.java` con anotaciones JPA.
2. **Repository**: `repository/CursoRepository.java` extendiendo `JpaRepository<Curso, Long>`.
3. **DTO**: `dto/CursoDTO.java` con validaciones Jakarta. **Solo si aplica** (formularios con campos sensibles, validaciones diferenciadas crear/editar, o necesidad de aplanar relaciones).
4. **Service**: `service/CursoService.java` con `@Transactional` y CRUD completo.
5. **Controller**: `controller/CursoController.java` con `@RequestMapping("/curso")` y soporte HTMX.
6. **Templates**:
   - `templates/curso/index.html` (decorador + tabla + Alpine.js + modal container)
   - `templates/curso/formulario.html` (modal con `th:fragment="formulario"`)
7. **Sidebar**: Agregar link en `layout/sidebar.html` con atributos HTMX.
8. **Compilar**: `mvn compile` para verificar.
