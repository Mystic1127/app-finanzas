package com.example.finanzas.data.model;

import java.util.Date;

public class Transaccion {
    public static final String INITIAL_BALANCE_CATEGORY = "Saldo inicial";
    public static final String INITIAL_BALANCE_CASH_NOTE = "Saldo inicial efectivo";
    public static final String INITIAL_BALANCE_CARD_NOTE = "Saldo inicial tarjeta/cuenta";
    public static final String INITIAL_BALANCE_ACCOUNT_NOTE_PREFIX = "Saldo inicial cuenta:";
    public static final String TRANSFER_CATEGORY = "Transferencia";
    public static final String TRANSFER_NOTE_PREFIX = "[transfer:";
    public static final String TRANSFER_NOTE_SEPARATOR = "] ";
    public static final String RECURRENT_NOTE_PREFIX = "[recurrent:";

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

    public boolean isTransfer() {
        String cleanCategory = categoriaNombre == null ? "" : categoriaNombre.trim();
        String cleanNote = nota == null ? "" : nota.trim();
        return TRANSFER_CATEGORY.equalsIgnoreCase(cleanCategory)
                || cleanNote.toLowerCase(java.util.Locale.ROOT).startsWith(TRANSFER_NOTE_PREFIX);
    }

    public String getTransferDestinationAccountType() {
        String cleanNote = nota == null ? "" : nota.trim();
        String lower = cleanNote.toLowerCase(java.util.Locale.ROOT);
        if (!lower.startsWith(TRANSFER_NOTE_PREFIX)) return "";
        int close = cleanNote.indexOf(']');
        if (close <= TRANSFER_NOTE_PREFIX.length()) return "";
        String inside = cleanNote.substring(TRANSFER_NOTE_PREFIX.length(), close);
        return normalizeAccountType(inside);
    }

    public String getDisplayNote() {
        String cleanNote = stripRecurringMarker(nota);
        if (!isTransfer()) return cleanNote;
        int close = cleanNote.indexOf(']');
        if (close < 0 || close + 1 >= cleanNote.length()) return "";
        return cleanNote.substring(close + 1).trim();
    }

    private static String stripRecurringMarker(String raw) {
        String clean = raw == null ? "" : raw.trim();
        if (!clean.toLowerCase(java.util.Locale.ROOT).startsWith(RECURRENT_NOTE_PREFIX)) return raw;
        int close = clean.indexOf(']');
        if (close < 0 || close + 1 >= clean.length()) return "";
        return clean.substring(close + 1).trim();
    }

    public static String buildTransferNote(String destinationAccountType, String userNote) {
        String destination = normalizeStaticAccountType(destinationAccountType);
        String cleanNote = userNote == null ? "" : userNote.trim();
        return TRANSFER_NOTE_PREFIX + destination + TRANSFER_NOTE_SEPARATOR + cleanNote;
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
        return normalizeStaticAccountType(value);
    }

    private static String normalizeStaticAccountType(String value) {
        if (value == null) return "CARD";
        String clean = value.trim();
        if (clean.isEmpty()) return "CARD";
        String upper = clean.toUpperCase(java.util.Locale.ROOT);
        if ("CASH".equals(upper) || "EFECTIVO".equals(upper)) return "CASH";
        if ("CARD".equals(upper) || "TARJETA".equals(upper) || "TARJETA/CUENTA".equals(upper)) return "CARD";
        return upper.replaceAll("[^A-Z0-9_:-]", "_");
    }
}
