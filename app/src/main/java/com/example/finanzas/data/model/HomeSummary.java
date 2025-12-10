package com.example.finanzas.data.model;

import java.util.ArrayList;
import java.util.List;

public class HomeSummary {
    private int anio;
    private int mes;
    private double ingresos;
    private double gastos;
    private double saldo;
    private double presupuestoMonto;
    private double presupuestoRestante;
    private double presupuestoPorcentaje;
    private boolean presupuestoExcedido;
    private double gastoProyectado;
    private double gastoPromedioDiario;
    private int diasRestantes;
    private String riesgoPresupuesto;
    private final List<CategoryBudgetSummary> presupuestosCategoria = new ArrayList<>();
    private final List<SavingsGoal> metas = new ArrayList<>();
    private final List<PaymentReminder> recordatorios = new ArrayList<>();
    private final List<String> alertas = new ArrayList<>();
    private final List<String> alertasPredictivas = new ArrayList<>();
    private final List<CategoryChartSlice> chartCategorias = new ArrayList<>();
    private final List<MonthlyTrendPoint> tendenciaMensual = new ArrayList<>();
    private int importacionesPendientes;
    private final List<HouseholdSummary> hogares = new ArrayList<>();
    private final List<ExchangeRate> tiposCambio = new ArrayList<>();
    private ConversionSummary conversion;
    private final List<DashboardModulePref> dashboardPreferencias = new ArrayList<>();
    private TravelPreference travelPreference;
    private final List<GamificationChallenge> gamificacionRetos = new ArrayList<>();

    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }

    public int getMes() { return mes; }
    public void setMes(int mes) { this.mes = mes; }

    public double getIngresos() { return ingresos; }
    public void setIngresos(double ingresos) { this.ingresos = ingresos; }

    public double getGastos() { return gastos; }
    public void setGastos(double gastos) { this.gastos = gastos; }

    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }

    public double getPresupuestoMonto() { return presupuestoMonto; }
    public void setPresupuestoMonto(double presupuestoMonto) { this.presupuestoMonto = presupuestoMonto; }

    public double getPresupuestoRestante() { return presupuestoRestante; }
    public void setPresupuestoRestante(double presupuestoRestante) { this.presupuestoRestante = presupuestoRestante; }

    public double getPresupuestoPorcentaje() { return presupuestoPorcentaje; }
    public void setPresupuestoPorcentaje(double presupuestoPorcentaje) { this.presupuestoPorcentaje = presupuestoPorcentaje; }

    public boolean isPresupuestoExcedido() { return presupuestoExcedido; }
    public void setPresupuestoExcedido(boolean presupuestoExcedido) { this.presupuestoExcedido = presupuestoExcedido; }

    public double getGastoProyectado() { return gastoProyectado; }
    public void setGastoProyectado(double gastoProyectado) { this.gastoProyectado = gastoProyectado; }

    public double getGastoPromedioDiario() { return gastoPromedioDiario; }
    public void setGastoPromedioDiario(double gastoPromedioDiario) { this.gastoPromedioDiario = gastoPromedioDiario; }

    public int getDiasRestantes() { return diasRestantes; }
    public void setDiasRestantes(int diasRestantes) { this.diasRestantes = diasRestantes; }

    public String getRiesgoPresupuesto() { return riesgoPresupuesto; }
    public void setRiesgoPresupuesto(String riesgoPresupuesto) { this.riesgoPresupuesto = riesgoPresupuesto; }

    public List<CategoryBudgetSummary> getPresupuestosCategoria() { return presupuestosCategoria; }

    public List<SavingsGoal> getMetas() { return metas; }

    public List<PaymentReminder> getRecordatorios() { return recordatorios; }

    public List<String> getAlertas() { return alertas; }

    public List<String> getAlertasPredictivas() { return alertasPredictivas; }

    public List<CategoryChartSlice> getChartCategorias() { return chartCategorias; }

    public List<MonthlyTrendPoint> getTendenciaMensual() { return tendenciaMensual; }

    public int getImportacionesPendientes() { return importacionesPendientes; }
    public void setImportacionesPendientes(int importacionesPendientes) { this.importacionesPendientes = importacionesPendientes; }

    public List<HouseholdSummary> getHogares() { return hogares; }

    public List<ExchangeRate> getTiposCambio() { return tiposCambio; }

    public ConversionSummary getConversion() { return conversion; }
    public void setConversion(ConversionSummary conversion) { this.conversion = conversion; }

    public List<DashboardModulePref> getDashboardPreferencias() { return dashboardPreferencias; }

    public TravelPreference getTravelPreference() { return travelPreference; }
    public void setTravelPreference(TravelPreference travelPreference) { this.travelPreference = travelPreference; }

    public List<GamificationChallenge> getGamificacionRetos() { return gamificacionRetos; }
}
