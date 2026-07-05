package com.example.finanzas.data.model;

public class CategorySuggestion {
    private Integer categoriaId;
    private String categoriaNombre;
    private String motivo;
    private double confidence;

    public Integer getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Integer categoriaId) { this.categoriaId = categoriaId; }

    public String getCategoriaNombre() { return categoriaNombre; }
    public void setCategoriaNombre(String categoriaNombre) { this.categoriaNombre = categoriaNombre; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public boolean hasSuggestion() { return categoriaId != null && categoriaId > 0; }
}
