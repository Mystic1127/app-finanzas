package com.example.finanzas.data.model;

import java.util.Date;

public class ReceiptDraft {
    private double monto;
    private Date fecha;
    private String comercio;
    private String moneda;

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }

    public String getComercio() { return comercio; }
    public void setComercio(String comercio) { this.comercio = comercio; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
}
