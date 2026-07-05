package com.example.finanzas.data.model;

public class ConversionSummary {
    private boolean enabled;
    private String monedaBase;
    private String monedaDestino;
    private double tasa;
    private double saldo;
    private double ingresos;
    private double gastos;
    private String actualizadoEn;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getMonedaBase() { return monedaBase; }
    public void setMonedaBase(String monedaBase) { this.monedaBase = monedaBase; }

    public String getMonedaDestino() { return monedaDestino; }
    public void setMonedaDestino(String monedaDestino) { this.monedaDestino = monedaDestino; }

    public double getTasa() { return tasa; }
    public void setTasa(double tasa) { this.tasa = tasa; }

    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }

    public double getIngresos() { return ingresos; }
    public void setIngresos(double ingresos) { this.ingresos = ingresos; }

    public double getGastos() { return gastos; }
    public void setGastos(double gastos) { this.gastos = gastos; }

    public String getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(String actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
