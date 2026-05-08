package com.example.finanzas.data.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.example.finanzas.data.model.GoalMilestone;

public class SavingsGoal {
    private int id;
    private String titulo;
    private double montoObjetivo;
    private double montoActual;
    private String moneda;
    private double progreso;
    private Date fechaObjetivo;
    private final List<GoalMilestone> hitos = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public double getMontoObjetivo() { return montoObjetivo; }
    public void setMontoObjetivo(double montoObjetivo) { this.montoObjetivo = montoObjetivo; }

    public double getMontoActual() { return montoActual; }
    public void setMontoActual(double montoActual) { this.montoActual = montoActual; }

    public String getMoneda() { return moneda == null || moneda.trim().isEmpty() ? "PEN" : moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }

    public double getProgreso() { return progreso; }
    public void setProgreso(double progreso) { this.progreso = progreso; }

    public Date getFechaObjetivo() { return fechaObjetivo; }
    public void setFechaObjetivo(Date fechaObjetivo) { this.fechaObjetivo = fechaObjetivo; }

    public List<GoalMilestone> getHitos() { return hitos; }
}
