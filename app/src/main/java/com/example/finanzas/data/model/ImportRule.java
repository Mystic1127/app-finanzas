package com.example.finanzas.data.model;

public class ImportRule {
    private int id;
    private String patron;
    private Integer categoriaId;
    private boolean ingreso;
    private String descripcion;
    private Boolean esIngreso;
    private String nota;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPatron() { return patron; }
    public void setPatron(String patron) { this.patron = patron; }

    public Integer getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Integer categoriaId) { this.categoriaId = categoriaId; }

    public boolean isIngreso() { return esIngreso != null ? esIngreso : ingreso; }
    public void setIngreso(boolean ingreso) { this.ingreso = ingreso; this.esIngreso = ingreso; }

    public boolean isEsIngreso() { return isIngreso(); }
    public void setEsIngreso(boolean esIngreso) { this.esIngreso = esIngreso; this.ingreso = esIngreso; }

    public String getDescripcion() { return descripcion != null ? descripcion : nota; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; this.nota = descripcion; }

    public String getNota() { return nota != null ? nota : descripcion; }
    public void setNota(String nota) { this.nota = nota; this.descripcion = nota; }
}
