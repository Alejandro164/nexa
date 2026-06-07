package com.empresa.proyecto.repository;

import com.empresa.proyecto.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Busca un usuario por email, nombre de usuario o cédula.
     * Usado por UserDetailsService para permitir login con cualquiera de los tres.
     */
    @Query("SELECT u FROM Usuario u WHERE " +
           "LOWER(u.email)   = LOWER(:identifier) OR " +
           "LOWER(u.usuario) = LOWER(:identifier) OR " +
           "u.cedula         = :identifier")
    Optional<Usuario> findByIdentifier(@Param("identifier") String identifier);

    @Query("SELECT u FROM Usuario u WHERE " +
           "LOWER(u.nombre) LIKE %:filtro% OR " +
           "LOWER(u.email)  LIKE %:filtro%")
    List<Usuario> findByNombreOrEmail(@Param("filtro") String filtro);

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.instituciones WHERE u.email = :email")
    Optional<Usuario> findByEmailWithInstituciones(@Param("email") String email);
}

