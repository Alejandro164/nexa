package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chavescr.nexa.entity.RegistroAcceso;

public interface RegistroAccesoRepository extends JpaRepository<RegistroAcceso, Long> {

    List<RegistroAcceso> findTop50ByOrderByFechaDesc();
}
