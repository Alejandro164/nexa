package com.chavescr.nexa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.PreferenciaUsuario;

@Repository
public interface PreferenciaUsuarioRepository extends JpaRepository<PreferenciaUsuario, Long> {

    Optional<PreferenciaUsuario> findByUsuarioId(Long usuarioId);
}
