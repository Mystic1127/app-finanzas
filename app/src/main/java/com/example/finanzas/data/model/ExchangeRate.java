package com.example.finanzas.data.model;

public class ExchangeRate {
    private String monedaBase;
    private String monedaDestino;
    private double tasa;
    private String actualizadoEn;

    public String getMonedaBase() { return monedaBase; }
    public void setMonedaBase(String monedaBase) { this.monedaBase = monedaBase; }

    public String getMonedaDestino() { return monedaDestino; }
    public void setMonedaDestino(String monedaDestino) { this.monedaDestino = monedaDestino; }

    public double getTasa() { return tasa; }
    public void setTasa(double tasa) { this.tasa = tasa; }

    public String getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(String actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
