package com.example.finanzas.data.model;

import java.util.ArrayList;
import java.util.List;

public class WeeklyPlanSummary {
    private double montoSemanal;
    private double gastoDiario;
    private double saldoActual;
    private double balanceMensual;
    private String estado;
    private String mensaje;
    private String detalleConfianza;
    private boolean preliminar;
    private boolean sinDatos;
    private final List<WeeklyPlanCategory> categorias = new ArrayList<>();
    private final List<String> advertencias = new ArrayList<>();

    public double getMontoSemanal() { return montoSemanal; }
    public void setMontoSemanal(double montoSemanal) { this.montoSemanal = montoSemanal; }

    public double getGastoDiario() { return gastoDiario; }
    public void setGastoDiario(double gastoDiario) { this.gastoDiario = gastoDiario; }

    public double getSaldoActual() { return saldoActual; }
    public void setSaldoActual(double saldoActual) { this.saldoActual = saldoActual; }

    public double getBalanceMensual() { return balanceMensual; }
    public void setBalanceMensual(double balanceMensual) { this.balanceMensual = balanceMensual; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getDetalleConfianza() { return detalleConfianza; }
    public void setDetalleConfianza(String detalleConfianza) { this.detalleConfianza = detalleConfianza; }

    public boolean isPreliminar() { return preliminar; }
    public boolean getPreliminar() { return preliminar; }
    public void setPreliminar(boolean preliminar) { this.preliminar = preliminar; }

    public boolean isSinDatos() { return sinDatos; }
    public boolean getSinDatos() { return sinDatos; }
    public void setSinDatos(boolean sinDatos) { this.sinDatos = sinDatos; }

    public List<WeeklyPlanCategory> getCategorias() { return categorias; }

    public List<String> getAdvertencias() { return advertencias; }
}
