package com.chavescr.nexa;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.chavescr.nexa.dto.DiaCalendarioDTO;
import com.chavescr.nexa.dto.EventoCalendarioDTO;
import com.chavescr.nexa.dto.EventoMepDTO;
import com.chavescr.nexa.entity.HorarioLeccion;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
import com.chavescr.nexa.entity.Recordatorio;
import com.chavescr.nexa.entity.Usuario;
import com.chavescr.nexa.service.ConfiguracionAcademicaService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProyectoApplicationTests {

	@Autowired
	private SpringTemplateEngine templateEngine;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void expiredHtmxRequestRedirectsTheWholePageToLogin() throws Exception {
		mockMvc.perform(get("/configuracion-academica/periodos/form")
						.header("HX-Request", "true"))
				.andExpect(status().isOk())
				.andExpect(header().string("HX-Redirect", "/login?expired"))
				.andExpect(content().string(""));
	}

	@Test
	void configuracionAcademicaTemplatesRender() {
		PeriodoAcademico periodo = new PeriodoAcademico();
		periodo.setId(1L);
		periodo.setCodigo("2026-I");
		periodo.setDescripcion("Primer período");
		periodo.setFechaInicio(LocalDate.of(2026, 2, 1));
		periodo.setFechaFin(LocalDate.of(2026, 6, 30));
		periodo.setActivo(true);

		NivelAcademico nivel = new NivelAcademico();
		nivel.setId(1L);
		nivel.setGrado("Sétimo");
		nivel.setSeccion("A");
		nivel.setActivo(true);

		Materia materia = new Materia();
		materia.setId(1L);
		materia.setCodigo("MAT-01");
		materia.setNombre("Matemáticas");
		materia.setArea("Ciencias exactas");
		materia.setTipo("Básica");
		materia.setColor("#2563eb");
		materia.setActivo(true);

		Usuario docente = new Usuario();
		docente.setId(1L);
		docente.setNombre("Docente de prueba");

		HorarioLeccion leccion = new HorarioLeccion();
		leccion.setId(1L);
		leccion.setPeriodo(periodo);
		leccion.setNivel(nivel);
		leccion.setMateria(materia);
		leccion.setDocente(docente);
		leccion.setDia("LUNES");
		leccion.setNumeroLeccion(1);
		leccion.setHoraInicio(LocalTime.of(7, 0));
		leccion.setHoraFin(LocalTime.of(7, 40));

		Context context = new Context();
		context.setVariable("periodos", List.of(periodo));
		context.setVariable("niveles", List.of(nivel));
		context.setVariable("materias", List.of(materia));
		context.setVariable("periodo", periodo);
		context.setVariable("nivel", nivel);
		context.setVariable("materia", materia);
		context.setVariable("periodosActivos", List.of(periodo));
		context.setVariable("nivelesActivos", List.of(nivel));
		context.setVariable("periodoSeleccionado", 1L);
		context.setVariable("nivelSeleccionado", 1L);
		context.setVariable("dias", ConfiguracionAcademicaService.DIAS);
		context.setVariable("lecciones", ConfiguracionAcademicaService.LECCIONES);
		context.setVariable("horario", Map.of("1-LUNES", leccion));
		context.setVariable("leccion", leccion);
		context.setVariable("periodoId", 1L);
		context.setVariable("nivelId", 1L);
		context.setVariable("docentes", List.of(docente));

		List<String> templates = List.of(
				"configuracion-academica/periodos/periodos",
				"configuracion-academica/periodos/form",
				"configuracion-academica/niveles/niveles",
				"configuracion-academica/niveles/form",
				"configuracion-academica/materias/materias",
				"configuracion-academica/materias/form",
				"configuracion-academica/horario/horario",
				"configuracion-academica/horario/form",
				"configuracion-academica/components/confirmar-eliminacion");

		templates.forEach(template -> assertFalse(templateEngine.process(template, context).isBlank()));

		String horarioRenderizado = templateEngine.process(
				"configuracion-academica/horario/horario", context);
		assertTrue(horarioRenderizado.contains("Matemáticas"));
		assertTrue(horarioRenderizado.contains("Docente de prueba"));
		assertTrue(horarioRenderizado.contains("schedule-slot schedule-slot-filled"));
		assertFalse(horarioRenderizado.contains(
				"periodoId=1&amp;amp;nivelId=1"));
	}

	@Test
	void recordatorioTemplatesRenderYReflejanEstado() {
		Recordatorio pendiente = new Recordatorio();
		pendiente.setId(1L);
		pendiente.setTitulo("Revisión de exámenes de reposición");
		pendiente.setDescripcion("Revisar notas pendientes");
		pendiente.setFechaLimite(LocalDate.of(2026, 7, 6).atTime(17, 0));
		pendiente.setEstado(Recordatorio.EstadoRecordatorio.PENDIENTE);

		Context context = new Context();
		context.setVariable("recordatorios", List.of(pendiente));
		context.setVariable("recordatorio", new Recordatorio());
		context.setVariable("error", null);

		String contenido = templateEngine.process("agenda/recordatorio/recordatorios", context);
		assertTrue(contenido.contains("Revisión de exámenes de reposición"));
		assertTrue(contenido.contains("tarea-badge-pendiente"));
		assertTrue(contenido.contains("/agenda/recordatorios/1/estado?estado=COMPLETADO"));

		pendiente.setEstado(Recordatorio.EstadoRecordatorio.COMPLETADO);
		context.setVariable("recordatorios", List.of(pendiente));
		String contenidoCompletado = templateEngine.process("agenda/recordatorio/recordatorios", context);
		assertTrue(contenidoCompletado.contains("tarea-badge-completada"));
		assertTrue(contenidoCompletado.contains("/agenda/recordatorios/1/estado?estado=PENDIENTE"));
	}

	@Test
	void expiredHtmxRequestOnRecordatoriosRedirectsToLogin() throws Exception {
		mockMvc.perform(get("/agenda/recordatorios/form")
						.header("HX-Request", "true"))
				.andExpect(status().isOk())
				.andExpect(header().string("HX-Redirect", "/login?expired"))
				.andExpect(content().string(""));
	}

	@Test
	void actividadInstitucionalTablaRenderaEventosDelMep() {
		EventoMepDTO destacado = new EventoMepDTO();
		destacado.setId("3");
		destacado.setTitulo("Inicio de curso lectivo para Colegios Científicos Costarricenses.");
		destacado.setDescripcion("Inicio del ciclo lectivo 2026.");
		destacado.setLink("https://www.mep.go.cr");
		destacado.setFechaInicio(LocalDate.of(2026, 2, 2));
		destacado.setFechaFin(LocalDate.of(2026, 2, 2));
		destacado.setNombreCategoria("Periodos Lectivos");
		destacado.setDestacado("1");

		EventoMepDTO enRango = new EventoMepDTO();
		enRango.setId("1");
		enRango.setTitulo("Matrícula de estudiantes repetidores.");
		enRango.setFechaInicio(LocalDate.of(2026, 1, 5));
		enRango.setFechaFin(LocalDate.of(2026, 1, 29));
		enRango.setNombreCategoria("Periodo de Matrícula");
		enRango.setDestacado("0");

		Context context = new Context();
		context.setVariable("eventos", List.of(destacado, enRango));

		String html = templateEngine.process(
				new TemplateSpec("agenda/actividad/actividad", Set.of("eventos-tabla"),
						(org.thymeleaf.templatemode.TemplateMode) null, null), context);

		assertTrue(html.contains("Inicio de curso lectivo para Colegios Científicos Costarricenses."));
		assertTrue(html.contains("Periodos Lectivos"));
		assertTrue(html.contains("Destacado"));
		assertTrue(html.contains("02/02/2026"));
		assertTrue(html.contains("05/01/2026 – 29/01/2026"));
		assertTrue(html.contains("Ver detalles"));

		context.setVariable("eventos", List.of());
		String htmlVacio = templateEngine.process(
				new TemplateSpec("agenda/actividad/actividad", Set.of("eventos-tabla"),
						(org.thymeleaf.templatemode.TemplateMode) null, null), context);
		assertTrue(htmlVacio.contains("No se encontraron actividades institucionales"));
	}

	@Test
	void actividadInstitucionalModalRenderaDetalleCompleto() {
		EventoMepDTO evento = new EventoMepDTO();
		evento.setId("3");
		evento.setTitulo("Inicio de curso lectivo para Colegios Científicos Costarricenses.");
		evento.setDescripcion("Inicio del ciclo lectivo 2026.");
		evento.setLink("https://www.mep.go.cr");
		evento.setFechaInicio(LocalDate.of(2026, 2, 2));
		evento.setFechaFin(LocalDate.of(2026, 2, 2));
		evento.setNombreCategoria("Periodos Lectivos");
		evento.setSubcategorias(List.of("Colegios Científicos Costarricenses", " "));
		evento.setDestacado("1");

		Context context = new Context();
		context.setVariable("evento", evento);

		String html = templateEngine.process(
				new TemplateSpec("agenda/actividad/actividad", Set.of("detalle-modal"),
						(org.thymeleaf.templatemode.TemplateMode) null, null), context);

		assertTrue(html.contains("Inicio de curso lectivo para Colegios Científicos Costarricenses."));
		assertTrue(html.contains("Inicio del ciclo lectivo 2026."));
		assertTrue(html.contains("Colegios Científicos Costarricenses"));
		assertTrue(html.contains("https://www.mep.go.cr"));
		assertTrue(html.contains("Destacado"));

		context.setVariable("evento", null);
		String htmlNoEncontrado = templateEngine.process(
				new TemplateSpec("agenda/actividad/actividad", Set.of("detalle-modal"),
						(org.thymeleaf.templatemode.TemplateMode) null, null), context);
		assertTrue(htmlNoEncontrado.contains("Actividad no encontrada"));
		assertTrue(htmlNoEncontrado.contains("No se encontró información"));
	}

	@Test
	void calendarioVistaMesRenderaEventosConTopeBandasYLabelsEnEspanol() {
		LocalDate lunes = LocalDate.of(2026, 7, 6);

		List<EventoCalendarioDTO> eventosLunes = List.of(
				EventoCalendarioDTO.deTarea("Tarea 1", "desc", lunes),
				EventoCalendarioDTO.deTarea("Tarea 2", "desc", lunes),
				EventoCalendarioDTO.deTarea("Tarea 3", "desc", lunes),
				EventoCalendarioDTO.deTarea("Tarea 4", "desc", lunes));
		DiaCalendarioDTO diaLunes = new DiaCalendarioDTO(lunes, false, true, eventosLunes);

		DiaCalendarioDTO diaMartes = new DiaCalendarioDTO(lunes.plusDays(1), false, false,
				List.of(EventoCalendarioDTO.deRecordatorio("Enviar reporte", "desc", lunes.plusDays(1),
						java.time.LocalTime.of(16, 30))));

		DiaCalendarioDTO diaMiercoles = new DiaCalendarioDTO(lunes.plusDays(2), false, false,
				List.of(EventoCalendarioDTO.deInstitucional("Feriado", "desc", lunes.plusDays(2), null, false)));

		List<DiaCalendarioDTO> dias = new java.util.ArrayList<>(List.of(diaLunes, diaMartes, diaMiercoles));
		for (int i = 3; i < 7; i++) {
			dias.add(new DiaCalendarioDTO(lunes.plusDays(i), false, false, List.of()));
		}

		List<com.chavescr.nexa.dto.BandaEventoDTO> bandas = List.of(
				new com.chavescr.nexa.dto.BandaEventoDTO("Semana de Exámenes", "desc", null, 1, 4, 0),
				new com.chavescr.nexa.dto.BandaEventoDTO("Feria Científica", "desc", null, 2, 3, 1));
		com.chavescr.nexa.dto.SemanaCalendarioDTO semana = new com.chavescr.nexa.dto.SemanaCalendarioDTO(dias, bandas);

		Context context = new Context();
		context.setVariable("vista", "mes");
		context.setVariable("semanas", List.of(semana));
		context.setVariable("miniSemanas", List.of(dias));
		context.setVariable("miniLabel", "Julio 2026");
		context.setVariable("miniAnterior", "2026-06-06");
		context.setVariable("miniSiguiente", "2026-08-06");
		context.setVariable("fechaRef", "2026-07-06");
		context.setVariable("tituloLabel", "Julio 2026");
		context.setVariable("hoyIso", "2026-07-06");
		context.setVariable("fechaAnterior", "2026-06-06");
		context.setVariable("fechaSiguiente", "2026-08-06");

		String html = templateEngine.process("agenda/calendario/calendario", context);

		assertTrue(html.contains("Julio 2026"));
		assertTrue(html.contains("Tarea 1"));
		assertTrue(html.contains("Tarea 2"));
		assertTrue(html.contains("Tarea 3"));
		assertFalse(html.contains("Tarea 4"));
		assertTrue(html.contains("+1 más"));
		assertTrue(html.contains("event-tarea"));
		assertTrue(html.contains("event-recordatorio"));
		assertTrue(html.contains("event-general"));
		assertTrue(html.contains("16:30"));
		assertTrue(html.contains("cal-banda"));
		assertTrue(html.contains("Semana de Exámenes"));
		assertTrue(html.contains("Feria Científica"));
		assertTrue(html.contains("grid-column:1 / 5"));
		assertTrue(html.contains("grid-row:1"));
		assertTrue(html.contains("grid-row:2"));
		assertFalse(html.contains("Marcar No Disponible"));
		assertFalse(html.contains("Salas Virtuales"));
	}
}
