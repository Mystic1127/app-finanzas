package com.example.finanzas.data.model;

import java.util.ArrayList;
import java.util.List;

public class HouseholdSummary {
    private int id;
    private String nombre;
    private String rol;
    private int miembros;
    private double ingresos;
    private double gastos;
    private double saldo;
    private final List<HouseholdMember> detalle = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public int getMiembros() { return miembros; }
    public void setMiembros(int miembros) { this.miembros = miembros; }

    public double getIngresos() { return ingresos; }
    public void setIngresos(double ingresos) { this.ingresos = ingresos; }

    public double getGastos() { return gastos; }
    public void setGastos(double gastos) { this.gastos = gastos; }

    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }

    public double getBalance() { return saldo; }
    public void setBalance(double balance) { this.saldo = balance; }

    public List<HouseholdMember> getDetalle() { return detalle; }

    public static class HouseholdMember {
        private int userId;
        private String nombre;
        private String rol;
        private double ingresos;
        private double gastos;
        private double saldo;

        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }

        public String getRol() { return rol; }
        public void setRol(String rol) { this.rol = rol; }

        public double getIngresos() { return ingresos; }
        public void setIngresos(double ingresos) { this.ingresos = ingresos; }

        public double getGastos() { return gastos; }
        public void setGastos(double gastos) { this.gastos = gastos; }

        public double getSaldo() { return saldo; }
        public void setSaldo(double saldo) { this.saldo = saldo; }
    }
}
