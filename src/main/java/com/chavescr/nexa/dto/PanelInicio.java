package com.chavescr.nexa.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Datos del dashboard de inicio. Cada rol ve un panel distinto: la dirección lo que pasa hoy en la
 * institución y lo que requiere su atención, el docente sus lecciones y pendientes, y la familia o el
 * estudiante cómo va en el período.
 */
public final class PanelInicio {

    private static final Locale ES = Locale.forLanguageTag("es-CR");
    private static final ZoneId ZONA = ZoneId.of("America/Costa_Rica");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("h:mm a", ES);

    private PanelInicio() {
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    /** Algo que espera una acción, con el enlace al módulo donde se resuelve. */
    public record Pendiente(String titulo, String detalle, long cantidad, String enlace, String tono) {
    }

    /** Período vigente y cuánto ha avanzado (0-100). */
    public record Periodo(String codigo, String descripcion, LocalDate inicio, LocalDate fin, long diasRestantes,
            int avance) {
    }

    /** Un evento de la agenda; si dura varios días aparece una sola vez con su rango. */
    public record EventoAgenda(LocalDate fecha, LocalDate fin, LocalTime hora, String titulo, String tipo,
            String tipoEtiqueta, String enlace) {

        public boolean isHoy() {
            return LocalDate.now(ZONA).equals(fecha);
        }

        public boolean isVariosDias() {
            return fin != null && fin.isAfter(fecha);
        }

        /** "lun", "mar"... para la etiqueta de la fecha. */
        public String getDiaSemanaCorto() {
            return fecha.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES).replace(".", "");
        }

        /** "Hoy", "Mañana" o el día de la semana; con el rango si dura varios días. */
        public String getCuando() {
            LocalDate hoy = LocalDate.now(ZONA);
            String dia = fecha.equals(hoy) ? "Hoy"
                    : fecha.equals(hoy.plusDays(1)) ? "Mañana"
                    : capitalizar(fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, ES));
            if (isVariosDias()) {
                dia += " al " + fin.getDayOfMonth() + " " + fin.getMonth().getDisplayName(TextStyle.SHORT, ES)
                        .replace(".", "");
            }
            return hora == null ? dia : dia + " · " + hora.format(HORA);
        }
    }

    /** Una lección del horario de hoy. */
    public record Leccion(Integer numero, LocalTime inicio, LocalTime fin, String materia, String seccion,
            String docente, String aula, boolean listaPasada, Estado estado) {

        public enum Estado { PROXIMA, EN_CURSO, TERMINADA }

        public boolean isSinLista() {
            return !listaPasada && estado != Estado.PROXIMA;
        }
    }

    /** Asistencia de estudiantes y personal del día. */
    public record AsistenciaHoy(long ausentes, long tardias, long justificadas, int leccionesDadas,
            int leccionesConLista, List<Leccion> sinLista, long personalPresente, boolean hayClases) {

        public int getPorcentajeConLista() {
            return leccionesDadas == 0 ? 100 : Math.round(leccionesConLista * 100f / leccionesDadas);
        }
    }

    public record Direccion(AsistenciaHoy asistencia, List<Pendiente> pendientes, Periodo periodo,
            List<EventoAgenda> agenda) {
    }

    /** Estudiante con una o más materias bajo la nota de aprobación en el período. */
    public record EstudianteEnRiesgo(Long id, String nombre, String seccion, String iniciales, String color,
            List<String> materias, double peorNota) {
    }

    public record SeccionPromedio(Long id, String seccion, double promedio, int estudiantes, int enRiesgo) {
    }

    public record Rendimiento(String periodo, int estudiantesConNotas, int totalEnRiesgo,
            List<EstudianteEnRiesgo> enRiesgo, List<SeccionPromedio> seccionesMasBajas, String aviso) {
    }

    /** Sección donde el docente es profesor guía, con quién tuvo novedades hoy. */
    public record SeccionGuia(Long id, String seccion, int estudiantes, long ausentesHoy, long tardiasHoy) {
    }

    public record Docente(List<Leccion> clases, List<Pendiente> pendientes, List<SeccionGuia> guia,
            Periodo periodo, List<EventoAgenda> agenda, String diaSinClases) {
    }

    /** Nota de una materia en el período. */
    public record MateriaNota(String nombre, double nota, boolean aprobada) {
    }

    /** Cómo va un estudiante en el período (para la familia o el propio estudiante). */
    public record Estudiante(Long id, String nombre, String seccion, String iniciales, String color,
            Double promedio, int notaAprobacion, List<MateriaNota> materias, Double conducta, int ausencias,
            int tardias,
            int llamadas, int boletas, int ausenciasHoy, int tardiasHoy) {

        public long getMateriasBajo() {
            return materias.stream().filter(m -> !m.aprobada()).count();
        }
    }

    public record Familia(List<Estudiante> estudiantes, List<Pendiente> pendientes, Periodo periodo,
            List<EventoAgenda> agenda, boolean esEstudiante) {
    }
}
