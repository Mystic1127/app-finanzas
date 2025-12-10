package com.example.finanzas.data.model;

import java.util.Date;

public class Transaccion {

    private int id;
    private int categoriaId;
    private String categoriaNombre;
    private boolean esIngreso;
    private double monto;
    private Date fecha;
    private String nota;

    public Transaccion() {
    }

    public Transaccion(int id, int categoriaId, String categoriaNombre,
                       boolean esIngreso, double monto, Date fecha, String nota) {
        this.id = id;
        this.categoriaId = categoriaId;
        this.categoriaNombre = categoriaNombre;
        this.esIngreso = esIngreso;
        this.monto = monto;
        this.fecha = fecha;
        this.nota = nota;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(int categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getCategoriaNombre() {
        return categoriaNombre;
    }

    public void setCategoriaNombre(String categoriaNombre) {
        this.categoriaNombre = categoriaNombre;
    }

    public boolean isEsIngreso() {
        return esIngreso;
    }

    public void setEsIngreso(boolean esIngreso) {
        this.esIngreso = esIngreso;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }


    @Override
    public String toString() {
        return "Transaccion{" +
                "id=" + id +
                ", categoriaId=" + categoriaId +
                ", categoriaNombre='" + categoriaNombre + '\'' +
                ", esIngreso=" + esIngreso +
                ", monto=" + monto +
                ", fecha=" + fecha +
                ", nota='" + nota + '\'' +
                '}';
    }
}
