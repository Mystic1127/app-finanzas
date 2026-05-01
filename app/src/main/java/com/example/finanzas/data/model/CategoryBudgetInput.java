package com.example.finanzas.data.model;

public class CategoryBudgetInput {
    private int categoriaId;
    private String categoriaNombre;
    private double monto;
    private String moneda;

    public int getCategoriaId() { return categoriaId; }
    public void setCategoriaId(int categoriaId) { this.categoriaId = categoriaId; }

    public String getCategoriaNombre() { return categoriaNombre; }
    public void setCategoriaNombre(String categoriaNombre) { this.categoriaNombre = categoriaNombre; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public String getMoneda() { return moneda == null || moneda.trim().isEmpty() ? "PEN" : moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
}
