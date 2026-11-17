package com.chavescr.nexa.entity;

/**
 * Cómo se calcula la asistencia de cada materia en el período.
 * <ul>
 * <li>{@link #REGLAMENTO_MEP}: tabla del artículo 37 del Reglamento de Evaluación de los Aprendizajes, según el
 * porcentaje de ausencias injustificadas sobre las lecciones impartidas. Va siempre al rubro Asistencia.</li>
 * <li>{@link #PERSONALIZADO}: el colegio define cuántos puntos baja cada grupo de ausencias o tardías, y si
 * rebajan el rubro Asistencia o el total de la conducta.</li>
 * </ul>
 */
public enum ModoAsistencia {
    REGLAMENTO_MEP,
    PERSONALIZADO
}
