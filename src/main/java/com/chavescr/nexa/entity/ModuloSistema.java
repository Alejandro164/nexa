package com.chavescr.nexa.entity;

/**
 * Módulos de la aplicación, alineados con las rutas del sidebar.
 * Se usa para filtrar la bitácora según la pantalla actual.
 */
public enum ModuloSistema {

    INICIO("/inicio", "Inicio"),
    CONTACTO("/contacto", "Contacto"),
    ESTUDIANTES("/estudiantes", "Estudiantes"),
    DOCENTES("/docentes", "Docentes"),
    PADRES("/padres", "Padres"),
    PERSONAL("/personal", "Acción de Personal"),
    CONTROL_ACCESO("/control-de-acceso", "Control de Acceso"),
    GESTION_ACADEMICA("/gestion-academica", "Gestión Académica"),
    GESTION_ESPECIAL("/gestion-especial", "Gestión Especial"),
    COORDINACION_ACADEMICA("/coordinacion-academica", "Coordinación Académica"),
    EVALUACION_ACADEMICA("/evaluacion-academica", "Evaluación Académica"),
    CONDUCTA("/conducta", "Conducta"),
    NOTAS("/notas", "Notas"),
    COMEDOR("/comedor", "Comedor"),
    NUBE("/nube-nexa", "Nube Nexa"),
    ARCHIVO_GRADUADOS("/archivo-graduados", "Archivo Graduados"),
    OFICIOS("/oficios", "Gestión de Oficios"),
    COMUNICACION("/comunicacion", "Comunicación"),
    AGENDA("/agenda", "Agenda"),
    PORTAL_PADRES("/portal-padres", "Portal Padres"),
    REPORTES("/reportes", "Reportes"),
    MIS_CURSOS("/mis-cursos", "Mis Cursos"),
    COMPONENTES("/componentes", "Componentes"),
    CONFIGURACION("/configuracion", "Configuración"),
    CONFIGURACION_ACADEMICA("/configuracion-academica", "Configuración Académica"),
    USUARIOS("/usuarios", "Usuarios"),
    SEGURIDAD("/seguridad", "Seguridad"),
    INSTITUCIONES("/instituciones", "Instituciones");

    private final String ruta;
    private final String etiqueta;

    ModuloSistema(String ruta, String etiqueta) {
        this.ruta = ruta;
        this.etiqueta = etiqueta;
    }

    public String getRuta() {
        return ruta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Resuelve el módulo a partir de la URL actual. Prefijo más largo gana. */
    public static ModuloSistema desdeRuta(String ruta) {
        if (ruta == null || ruta.isBlank() || "/".equals(ruta)) {
            return INICIO;
        }
        String path = ruta.startsWith("/") ? ruta : "/" + ruta;
        ModuloSistema mejor = INICIO;
        int mejorLen = 0;
        for (ModuloSistema modulo : values()) {
            String prefijo = modulo.ruta;
            if (path.equals(prefijo) || path.startsWith(prefijo + "/")) {
                if (prefijo.length() > mejorLen) {
                    mejor = modulo;
                    mejorLen = prefijo.length();
                }
            }
        }
        return mejor;
    }

    public static String areaDe(ModuloAcademico academico) {
        if (academico == null) {
            return null;
        }
        return switch (academico) {
            case COTIDIANO -> "Cotidiano";
            case TAREA -> "Tareas";
            case PROYECTO -> "Proyectos";
            case EXAMEN -> "Exámenes";
            case EXTRACLASE -> "Extraclase";
        };
    }
}
