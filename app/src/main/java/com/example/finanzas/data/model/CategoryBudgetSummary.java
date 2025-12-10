package com.example.finanzas.data.model;

public class CategoryBudgetSummary {
    private int categoriaId;
    private String categoriaNombre;
    private double limite;
    private double gastado;
    private double disponible;
    private double porcentaje;
    private boolean alerta;

    public int getCategoriaId() { return categoriaId; }
    public void setCategoriaId(int categoriaId) { this.categoriaId = categoriaId; }

    public String getCategoriaNombre() { return categoriaNombre; }
    public void setCategoriaNombre(String categoriaNombre) { this.categoriaNombre = categoriaNombre; }

    public double getLimite() { return limite; }
    public void setLimite(double limite) { this.limite = limite; }

    public double getGastado() { return gastado; }
    public void setGastado(double gastado) { this.gastado = gastado; }

    public double getDisponible() { return disponible; }
    public void setDisponible(double disponible) { this.disponible = disponible; }

    public double getPorcentaje() { return porcentaje; }
    public void setPorcentaje(double porcentaje) { this.porcentaje = porcentaje; }

    public boolean isAlerta() { return alerta; }
    public void setAlerta(boolean alerta) { this.alerta = alerta; }
}
