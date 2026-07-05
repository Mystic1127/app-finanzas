package com.example.finanzas.data.model;

public class CategoryBudgetInput {
    private int categoriaId;
    private String categoriaNombre;
    private double monto;
    private String moneda;
    private double gastado;
    private double disponible;
    private double porcentaje;

    public int getCategoriaId() { return categoriaId; }
    public void setCategoriaId(int categoriaId) { this.categoriaId = categoriaId; }

    public String getCategoriaNombre() { return categoriaNombre; }
    public void setCategoriaNombre(String categoriaNombre) { this.categoriaNombre = categoriaNombre; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public String getMoneda() { return moneda == null || moneda.trim().isEmpty() ? "PEN" : moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }

    public double getGastado() { return gastado; }
    public void setGastado(double gastado) { this.gastado = gastado; }

    public double getDisponible() { return disponible; }
    public void setDisponible(double disponible) { this.disponible = disponible; }

    public double getPorcentaje() { return porcentaje; }
    public void setPorcentaje(double porcentaje) { this.porcentaje = porcentaje; }
}
