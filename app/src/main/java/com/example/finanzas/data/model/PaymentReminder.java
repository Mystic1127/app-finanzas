package com.example.finanzas.data.model;

import java.util.Date;

public class PaymentReminder {
    private int id;
    private String titulo;
    private double monto;
    private Date fechaVencimiento;
    private int diasRestantes;
    private boolean pagado;
    private Integer categoriaId;
    private String horaRecordatorio;
    private String frecuencia;
    private boolean notificar;
    private int diasRecordatorio;
    private String googleEventId;
    private String notificationId;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public Date getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(Date fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public int getDiasRestantes() { return diasRestantes; }
    public void setDiasRestantes(int diasRestantes) { this.diasRestantes = diasRestantes; }

    public boolean isPagado() { return pagado; }
    public boolean getPagado() { return pagado; }
    public void setPagado(boolean pagado) { this.pagado = pagado; }

    public Integer getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Integer categoriaId) { this.categoriaId = categoriaId; }

    public String getHoraRecordatorio() { return horaRecordatorio; }
    public void setHoraRecordatorio(String horaRecordatorio) { this.horaRecordatorio = horaRecordatorio; }

    public String getFrecuencia() { return frecuencia; }
    public void setFrecuencia(String frecuencia) { this.frecuencia = frecuencia; }

    public boolean isNotificar() { return notificar; }
    public boolean getNotificar() { return notificar; }
    public void setNotificar(boolean notificar) { this.notificar = notificar; }

    public int getDiasRecordatorio() { return diasRecordatorio; }
    public void setDiasRecordatorio(int diasRecordatorio) { this.diasRecordatorio = diasRecordatorio; }

    public String getGoogleEventId() { return googleEventId; }
    public void setGoogleEventId(String googleEventId) { this.googleEventId = googleEventId; }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
}
