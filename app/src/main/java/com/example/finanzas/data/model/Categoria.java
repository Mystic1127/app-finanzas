package com.example.finanzas.data.model;

public class Categoria {
    public int id;
    public int userId;
    public String nombre;
    public boolean esIngreso;
    public boolean editable;

    public Categoria() { }

    public Categoria(int id, String nombre, boolean esIngreso) {
        this(id, 0, nombre, esIngreso, false);
    }

    public Categoria(int id, int userId, String nombre, boolean esIngreso, boolean editable) {
        this.id = id;
        this.userId = userId;
        this.nombre = nombre;
        this.esIngreso = esIngreso;
        this.editable = editable;
    }

    @Override
    public String toString() {
        return nombre == null ? "" : nombre;
    }
}
