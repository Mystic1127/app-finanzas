package com.example.finanzas.data.model;

public class Categoria {
    public int id;
    public String nombre;
    public boolean esIngreso;

    public Categoria() { }

    public Categoria(int id, String nombre, boolean esIngreso) {
        this.id = id;
        this.nombre = nombre;
        this.esIngreso = esIngreso;
    }

    @Override
    public String toString() {
        return nombre == null ? "" : nombre;
    }
}
