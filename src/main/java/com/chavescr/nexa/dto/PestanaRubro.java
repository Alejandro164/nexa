package com.chavescr.nexa.dto;

import com.chavescr.nexa.entity.ClaveComponente;

/**
 * Pestaña de un rubro en Gestión Académica para la materia elegida: solo existen las de los rubros que valen
 * más de 0% en el componente de evaluación de la materia, con el nombre que ese componente les dio.
 */
public class PestanaRubro {

    private final ClaveComponente clave;
    private final String nombre;
    private final String emoji;
    private final int porcentaje;
    private final String url;
    private final String panelId;

    /**
     * @param url     panel que se carga al abrir la pestaña (sin parámetros)
     * @param panelId id del contenedor del panel; los eventos de "calificado" lo recargan por este id
     */
    public PestanaRubro(ClaveComponente clave, String nombre, String emoji, int porcentaje, String url,
            String panelId) {
        this.clave = clave;
        this.nombre = nombre;
        this.emoji = emoji;
        this.porcentaje = porcentaje;
        this.url = url;
        this.panelId = panelId;
    }

    public String getId() {
        return clave.name();
    }

    public ClaveComponente getClave() {
        return clave;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmoji() {
        return emoji;
    }

    public int getPorcentaje() {
        return porcentaje;
    }

    public String getUrl() {
        return url;
    }

    public String getPanelId() {
        return panelId;
    }

    public String getEtiqueta() {
        return emoji == null || emoji.isBlank() ? nombre : emoji + " " + nombre;
    }
}
