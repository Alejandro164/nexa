package com.chavescr.nexa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.ClassroomAdjunto;

@Repository
public interface ClassroomAdjuntoRepository extends JpaRepository<ClassroomAdjunto, Long> {
}
