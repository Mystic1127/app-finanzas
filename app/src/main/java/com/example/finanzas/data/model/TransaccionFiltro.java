package com.example.finanzas.data.model;

import androidx.annotation.Nullable;

public class TransaccionFiltro {
    public enum Orden { FECHA, NOMBRE, CATEGORIA, MONTO }
    public enum Tipo { TODAS, INGRESOS, GASTOS, TRANSFERENCIAS }

    @Nullable private Long fechaInicio;
    @Nullable private Long fechaFin;
    @Nullable private Integer categoriaId;
    @Nullable private Orden orden;
    @Nullable private Tipo tipo;
    @Nullable private String accountType;
    @Nullable private Double montoMin;
    @Nullable private Double montoMax;
    @Nullable private String texto;
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

    @Nullable
    public Tipo getTipo() { return tipo; }

    public void setTipo(@Nullable Tipo tipo) { this.tipo = tipo; }

    @Nullable
    public String getAccountType() { return accountType; }

    public void setAccountType(@Nullable String accountType) { this.accountType = accountType; }

    @Nullable
    public Double getMontoMin() { return montoMin; }

    public void setMontoMin(@Nullable Double montoMin) { this.montoMin = montoMin; }

    @Nullable
    public Double getMontoMax() { return montoMax; }

    public void setMontoMax(@Nullable Double montoMax) { this.montoMax = montoMax; }

    @Nullable
    public String getTexto() { return texto; }

    public void setTexto(@Nullable String texto) { this.texto = texto; }

    public boolean isAscendente() { return ascendente; }

    public void setAscendente(boolean ascendente) { this.ascendente = ascendente; }
}
