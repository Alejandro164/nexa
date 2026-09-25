package com.chavescr.nexa.dto;

public class VistaComponente {

    private final String panelId;
    private final String listUrl;
    private final String formUrl;
    private final String evalUrl;
    private final String evalParam;
    private final String etiqueta;
    private final String botonNuevo;
    private final String mensajeVacio;
    private final String mensajeSinSeccion;

    public VistaComponente(String panelId, String listUrl, String formUrl, String evalUrl, String evalParam,
            String etiqueta, String botonNuevo, String mensajeVacio, String mensajeSinSeccion) {
        this.panelId = panelId;
        this.listUrl = listUrl;
        this.formUrl = formUrl;
        this.evalUrl = evalUrl;
        this.evalParam = evalParam;
        this.etiqueta = etiqueta;
        this.botonNuevo = botonNuevo;
        this.mensajeVacio = mensajeVacio;
        this.mensajeSinSeccion = mensajeSinSeccion;
    }

    public String getPanelId() {
        return panelId;
    }

    public String getListUrl() {
        return listUrl;
    }

    public String getFormUrl() {
        return formUrl;
    }

    public String getEvalUrl() {
        return evalUrl;
    }

    public String getEvalParam() {
        return evalParam;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getBotonNuevo() {
        return botonNuevo;
    }

    public String getMensajeVacio() {
        return mensajeVacio;
    }

    public String getMensajeSinSeccion() {
        return mensajeSinSeccion;
    }
}
