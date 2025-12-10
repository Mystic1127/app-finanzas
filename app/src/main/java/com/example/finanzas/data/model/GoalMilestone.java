package com.example.finanzas.data.model;

import java.util.Date;

public class GoalMilestone {
    private int id;
    private int metaId;
    private String titulo;
    private double montoPlanificado;
    private Date fechaObjetivo;
    private boolean notificar;
    private int diasRecordatorio;
    private boolean completado;
    private Integer diasRestantes;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMetaId() { return metaId; }
    public void setMetaId(int metaId) { this.metaId = metaId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public double getMontoPlanificado() { return montoPlanificado; }
    public void setMontoPlanificado(double montoPlanificado) { this.montoPlanificado = montoPlanificado; }

    public Date getFechaObjetivo() { return fechaObjetivo; }
    public void setFechaObjetivo(Date fechaObjetivo) { this.fechaObjetivo = fechaObjetivo; }

    public boolean isNotificar() { return notificar; }
    public void setNotificar(boolean notificar) { this.notificar = notificar; }

    public int getDiasRecordatorio() { return diasRecordatorio; }
    public void setDiasRecordatorio(int diasRecordatorio) { this.diasRecordatorio = diasRecordatorio; }

    public boolean isCompletado() { return completado; }
    public void setCompletado(boolean completado) { this.completado = completado; }

    public Integer getDiasRestantes() { return diasRestantes; }
    public void setDiasRestantes(Integer diasRestantes) { this.diasRestantes = diasRestantes; }
}
