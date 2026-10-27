package com.chavescr.nexa.dto;

import java.util.List;

/** Una página de filas de entregas académicas (la tabla muestra "pagina / totalPaginas"). */
public class PaginaEntregas {

    private final List<FilaEntregaAcademica> filas;
    private final int pagina;
    private final int totalPaginas;
    private final int total;

    public PaginaEntregas(List<FilaEntregaAcademica> filas, int pagina, int totalPaginas, int total) {
        this.filas = filas;
        this.pagina = pagina;
        this.totalPaginas = totalPaginas;
        this.total = total;
    }

    public List<FilaEntregaAcademica> getFilas() { return filas; }
    public int getPagina() { return pagina; }
    public int getTotalPaginas() { return totalPaginas; }
    public int getTotal() { return total; }
    public boolean isHayAnterior() { return pagina > 1; }
    public boolean isHaySiguiente() { return pagina < totalPaginas; }
}
