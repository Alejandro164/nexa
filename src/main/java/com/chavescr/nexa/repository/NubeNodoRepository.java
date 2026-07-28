package com.chavescr.nexa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.chavescr.nexa.entity.NubeNodo;

@Repository
public interface NubeNodoRepository extends JpaRepository<NubeNodo, Long> {

    // Find all nodes inside a specific parent folder (excluye los que están en la papelera)
    List<NubeNodo> findByPadreIdAndFechaEliminacionIsNullOrderByTipoAscNombreAsc(Long padreId);

    // Find all root nodes (where parent is null), excluye los que están en la papelera
    List<NubeNodo> findByPadreIsNullAndFechaEliminacionIsNullOrderByTipoAscNombreAsc();

    // Raíces de subárboles eliminados: nodos en la papelera cuyo padre no está también en la papelera.
    // LEFT JOIN explícito: un JOIN implícito vía "n.padre.fechaEliminacion" generaría un INNER JOIN,
    // excluyendo los nodos raíz (padre_id NULL) antes de evaluar el WHERE.
    @Query("SELECT n FROM NubeNodo n LEFT JOIN n.padre p WHERE n.fechaEliminacion IS NOT NULL " +
            "AND (p IS NULL OR p.fechaEliminacion IS NULL) " +
            "ORDER BY n.fechaEliminacion DESC")
    List<NubeNodo> findRaicesEnPapelera();

    // Nodos abiertos recientemente (excluye los que están en la papelera), más reciente primero
    List<NubeNodo> findTop50ByFechaEliminacionIsNullAndUltimoAccesoIsNotNullOrderByUltimoAccesoDesc();
}
