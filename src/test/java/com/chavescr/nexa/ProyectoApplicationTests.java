package com.chavescr.nexa;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.chavescr.nexa.entity.HorarioLeccion;
import com.chavescr.nexa.entity.Materia;
import com.chavescr.nexa.entity.NivelAcademico;
import com.chavescr.nexa.entity.PeriodoAcademico;
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
}
