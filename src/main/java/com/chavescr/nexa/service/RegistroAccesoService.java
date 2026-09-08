package com.chavescr.nexa.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chavescr.nexa.entity.AccionAcceso;
import com.chavescr.nexa.entity.RegistroAcceso;
import com.chavescr.nexa.entity.ResultadoAcceso;
import com.chavescr.nexa.repository.RegistroAccesoRepository;

import jakarta.servlet.http.HttpServletRequest;

/** Bitácora de accesos (login/logout, éxito/fallo) para el Centro de Seguridad. */
@Service
@Transactional
public class RegistroAccesoService {

    private final RegistroAccesoRepository repository;

    public RegistroAccesoService(RegistroAccesoRepository repository) {
        this.repository = repository;
    }

    public void registrar(String usuarioIdentificador, String ip, AccionAcceso accion, ResultadoAcceso resultado) {
        RegistroAcceso registro = new RegistroAcceso();
        registro.setFecha(LocalDateTime.now());
        registro.setUsuarioIdentificador(usuarioIdentificador);
        registro.setIp(ip);
        registro.setAccion(accion);
        registro.setResultado(resultado);
        repository.save(registro);
    }

    @Transactional(readOnly = true)
    public List<RegistroAcceso> listarRecientes() {
        return repository.findTop50ByOrderByFechaDesc();
    }

    /** IP real del cliente: respeta X-Forwarded-For si hay un proxy delante (ej. en producción). */
    public static String resolverIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
