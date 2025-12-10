package com.example.finanzas.data.model;

import androidx.annotation.Nullable;

public class TransaccionFiltro {
    public enum Orden { FECHA, NOMBRE, CATEGORIA }

    @Nullable private Long fechaInicio;
    @Nullable private Long fechaFin;
    @Nullable private Integer categoriaId;
    @Nullable private Orden orden;
    private boolean ascendente;

    @Nullable
    public Long getFechaInicio() { return fechaInicio; }

    public void setFechaInicio(@Nullable Long fechaInicio) { this.fechaInicio = fechaInicio; }

    @Nullable
    public Long getFechaFin() { return fechaFin; }

    public void setFechaFin(@Nullable Long fechaFin) { this.fechaFin = fechaFin; }

    @Nullable
    public Integer getCategoriaId() { return categoriaId; }

    public void setCategoriaId(@Nullable Integer categoriaId) { this.categoriaId = categoriaId; }

    @Nullable
    public Orden getOrden() { return orden; }

    public void setOrden(@Nullable Orden orden) { this.orden = orden; }

    public boolean isAscendente() { return ascendente; }

    public void setAscendente(boolean ascendente) { this.ascendente = ascendente; }
}
