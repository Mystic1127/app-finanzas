package com.example.finanzas.data.model;

public class CategoryChartSlice {
    private String categoriaNombre;
    private double presupuesto;
    private double gastado;

    public String getCategoriaNombre() {
        return categoriaNombre;
    }

    public void setCategoriaNombre(String categoriaNombre) {
        this.categoriaNombre = categoriaNombre;
    }

    public double getPresupuesto() {
        return presupuesto;
    }

    public void setPresupuesto(double presupuesto) {
        this.presupuesto = presupuesto;
    }

    public double getGastado() {
        return gastado;
    }

    public void setGastado(double gastado) {
        this.gastado = gastado;
    }
}
