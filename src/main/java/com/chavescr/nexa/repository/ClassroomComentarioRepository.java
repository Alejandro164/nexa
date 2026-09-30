package com.chavescr.nexa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.ClassroomComentario;

@Repository
public interface ClassroomComentarioRepository extends JpaRepository<ClassroomComentario, Long> {
}
