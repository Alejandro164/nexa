package com.chavescr.nexa.dto;

import java.nio.file.Path;
import java.util.List;

/** Datos ya preparados para pintar Comunicados. Los registros se leen en las plantillas con la sintaxis de método. */
public final class ComunicadosVista {

    private ComunicadosVista() {
    }

    /** Fila del panel de destinatarios (solo administración y dirección). {@code clave}: "COMUNIDAD", "SECCION-12", "PERSONA-50". */
    public record Destinatario(String clave, boolean grupo, String nombre, String detalle, String iniciales,
            String claseRol, long oficios, boolean pendiente) {
    }

    /** Persona encontrada al elegir destinatarios en el formulario de oficios. */
    public record Persona(Long id, String nombre, String detalle, String iniciales, String claseRol) {
    }

    public record Archivo(String nombre, String tamano, boolean word) {
    }

    /**
     * Estado de lectura. En un grupo cuenta cuántas personas del total lo han leído; en una persona solo importa
     * si lo leyó y cuándo ({@code fecha}).
     */
    public record Lectura(boolean grupo, long leidos, long total, String fecha) {

        public int porcentaje() {
            return total == 0 ? 0 : (int) Math.round(leidos * 100.0 / total);
        }

        public long pendientes() {
            return Math.max(0, total - leidos);
        }

        public boolean completa() {
            return total > 0 && leidos >= total;
        }

        public boolean leido() {
            return fecha != null;
        }
    }

    public record Oficio(Long id, String numero, String circular, String asunto, String redactor, String cuando,
            String mes, boolean nuevo, Archivo archivo, Lectura lectura) {
    }

    /** Lo que se ve a la derecha: el destinatario elegido y sus oficios. */
    public record Panel(String clave, String titulo, String subtitulo, String iniciales, String claseRol,
            boolean grupo, boolean propio, long sinLeer, List<Oficio> oficios, List<Integer> anios, Integer anio,
            String tipo, String busqueda) {

        public int total() {
            return oficios.size();
        }

        /** En un grupo se cuentan los oficios con lectura pendiente; en una persona, los que no ha leído. */
        public String etiquetaSinLeer() {
            return grupo ? "pendientes" : "sin leer";
        }
    }

    /** Documento del oficio listo para servirse. */
    public record Documento(Path ruta, String nombre, String tipoContenido, boolean enLinea) {
    }
}
