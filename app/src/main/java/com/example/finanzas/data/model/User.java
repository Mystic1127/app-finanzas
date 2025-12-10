package com.example.finanzas.data.model;

public class User {
    public int id;
    public String nombre;
    public String email;

    public User() {}

    public User(int id, String nombre, String email) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
    }
}
