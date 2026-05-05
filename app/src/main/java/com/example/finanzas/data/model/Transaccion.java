package com.example.finanzas.data.model;

import java.util.Date;

public class Transaccion {
    public static final String INITIAL_BALANCE_CATEGORY = "Saldo inicial";
    public static final String INITIAL_BALANCE_CASH_NOTE = "Saldo inicial efectivo";
    public static final String INITIAL_BALANCE_CARD_NOTE = "Saldo inicial tarjeta/cuenta";
    public static final String INITIAL_BALANCE_ACCOUNT_NOTE_PREFIX = "Saldo inicial cuenta:";

    private int id;
    private int categoriaId;
    private String categoriaNombre;
    private boolean esIngreso;
    private double monto;
    private String moneda;
    private Date fecha;
    private String accountType;
    private String nota;

    public Transaccion() {
    }

    public Transaccion(int id, int categoriaId, String categoriaNombre,
                       boolean esIngreso, double monto, Date fecha, String nota) {
        this(id, categoriaId, categoriaNombre, esIngreso, monto, "PEN", fecha, nota);
    }

    public Transaccion(int id, int categoriaId, String categoriaNombre,
                       boolean esIngreso, double monto, String moneda, Date fecha, String nota) {
        this(id, categoriaId, categoriaNombre, esIngreso, monto, moneda, fecha, "CARD", nota);
    }

    public Transaccion(int id, int categoriaId, String categoriaNombre,
                       boolean esIngreso, double monto, String moneda, Date fecha, String accountType, String nota) {
        this.id = id;
        this.categoriaId = categoriaId;
        this.categoriaNombre = categoriaNombre;
        this.esIngreso = esIngreso;
        this.monto = monto;
        this.moneda = moneda == null || moneda.trim().isEmpty() ? "PEN" : moneda;
        this.fecha = fecha;
        this.accountType = normalizeAccountType(accountType);
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

    public String getAccountType() {
        return accountType == null || accountType.trim().isEmpty() ? "CARD" : accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = normalizeAccountType(accountType);
    }

    public boolean isCash() {
        return "CASH".equalsIgnoreCase(getAccountType());
    }

    public boolean isInitialBalance() {
        if (!esIngreso) return false;
        boolean specialCategory = INITIAL_BALANCE_CATEGORY.equalsIgnoreCase(
                categoriaNombre == null ? "" : categoriaNombre.trim()
        );
        String cleanNote = nota == null ? "" : nota.trim();
        boolean specialNote = INITIAL_BALANCE_CASH_NOTE.equalsIgnoreCase(cleanNote)
                || INITIAL_BALANCE_CARD_NOTE.equalsIgnoreCase(cleanNote)
                || INITIAL_BALANCE_CATEGORY.equalsIgnoreCase(cleanNote)
                || cleanNote.toLowerCase(java.util.Locale.ROOT).startsWith(
                INITIAL_BALANCE_ACCOUNT_NOTE_PREFIX.toLowerCase(java.util.Locale.ROOT)
        );
        return specialCategory && specialNote;
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
                ", accountType='" + accountType + '\'' +
                ", nota='" + nota + '\'' +
                '}';
    }

    private String normalizeAccountType(String value) {
        if (value == null) return "CARD";
        String clean = value.trim();
        if (clean.isEmpty()) return "CARD";
        String upper = clean.toUpperCase(java.util.Locale.ROOT);
        if ("CASH".equals(upper) || "EFECTIVO".equals(upper)) return "CASH";
        if ("CARD".equals(upper) || "TARJETA".equals(upper) || "TARJETA/CUENTA".equals(upper)) return "CARD";
        return upper.replaceAll("[^A-Z0-9_:-]", "_");
    }
}
