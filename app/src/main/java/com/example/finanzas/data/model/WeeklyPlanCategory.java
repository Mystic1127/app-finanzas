package com.example.finanzas.data.model;

public class WeeklyPlanCategory {
    private String nombre;
    private double monto;
    private boolean limitadoPorPresupuesto;

    public WeeklyPlanCategory() {}

    public WeeklyPlanCategory(String nombre, double monto, boolean limitadoPorPresupuesto) {
        this.nombre = nombre;
        this.monto = monto;
        this.limitadoPorPresupuesto = limitadoPorPresupuesto;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public boolean isLimitadoPorPresupuesto() { return limitadoPorPresupuesto; }
    public boolean getLimitadoPorPresupuesto() { return limitadoPorPresupuesto; }
    public void setLimitadoPorPresupuesto(boolean limitadoPorPresupuesto) { this.limitadoPorPresupuesto = limitadoPorPresupuesto; }
}
