package com.example.finanzas.data.model;

public class AccountBalance {
    private String id;
    private String name;
    private double balance;
    private boolean primary;

    public AccountBalance() {
    }

    public AccountBalance(String id, String name, double balance, boolean primary) {
        this.id = id;
        this.name = name;
        this.balance = balance;
        this.primary = primary;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean isPrimary() {
        return primary;
    }

    public boolean getPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}
