package com.example.finanzas.data.model;

import java.util.Date;

public class Transaccion {

    private int id;
    private int categoriaId;
    private String categoriaNombre;
    private boolean esIngreso;
    private double monto;
    private String moneda;
    private Date fecha;
    private String nota;

    public Transaccion() {
    }

    public Transaccion(int id, int categoriaId, String categoriaNombre,
                       boolean esIngreso, double monto, Date fecha, String nota) {
        this(id, categoriaId, categoriaNombre, esIngreso, monto, "PEN", fecha, nota);
    }

    public Transaccion(int id, int categoriaId, String categoriaNombre,
                       boolean esIngreso, double monto, String moneda, Date fecha, String nota) {
        this.id = id;
        this.categoriaId = categoriaId;
        this.categoriaNombre = categoriaNombre;
        this.esIngreso = esIngreso;
        this.monto = monto;
        this.moneda = moneda == null || moneda.trim().isEmpty() ? "PEN" : moneda;
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

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
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
                ", moneda='" + moneda + '\'' +
                ", fecha=" + fecha +
                ", nota='" + nota + '\'' +
                '}';
    }
}
