package com.example.finanzas.data.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ImportJob {
    private int id;
    private String nombre;
    private String tipo;
    private String nombreArchivo;
    private String estado;
    private int total;
    private int procesados;
    private int errores;
    private int pendientes;
    private int procesadas;
    private int omitidas;
    private String mensajeError;
    private Date createdAt;
    private Date processedAt;
    private final List<ImportLine> preview = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre != null ? nombre : nombreArchivo; }
    public void setNombre(String nombre) { this.nombre = nombre; this.nombreArchivo = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; this.nombre = nombreArchivo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getProcesados() { return procesados; }
    public void setProcesados(int procesados) { this.procesados = procesados; }

    public int getErrores() { return errores; }
    public void setErrores(int errores) { this.errores = errores; }

    public int getPendientes() { return pendientes; }
    public void setPendientes(int pendientes) { this.pendientes = pendientes; }

    public int getProcesadas() { return procesadas; }
    public void setProcesadas(int procesadas) { this.procesadas = procesadas; }

    public int getOmitidas() { return omitidas; }
    public void setOmitidas(int omitidas) { this.omitidas = omitidas; }

    public String getMensajeError() { return mensajeError; }
    public void setMensajeError(String mensajeError) { this.mensajeError = mensajeError; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getProcessedAt() { return processedAt; }
    public void setProcessedAt(Date processedAt) { this.processedAt = processedAt; }

    public List<ImportLine> getPreview() { return preview; }

    public static class ImportLine {
        private String fecha;
        private String descripcion;
        private double monto;
        private boolean ingreso;
        private Integer categoriaId;

        public String getFecha() { return fecha; }
        public void setFecha(String fecha) { this.fecha = fecha; }

        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

        public double getMonto() { return monto; }
        public void setMonto(double monto) { this.monto = monto; }

        public boolean isIngreso() { return ingreso; }
        public void setIngreso(boolean ingreso) { this.ingreso = ingreso; }

        public Integer getCategoriaId() { return categoriaId; }
        public void setCategoriaId(Integer categoriaId) { this.categoriaId = categoriaId; }
    }
}
