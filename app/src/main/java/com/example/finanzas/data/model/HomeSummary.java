package com.example.finanzas.data.model;

import java.util.ArrayList;
import java.util.List;

public class HomeSummary {
    private int anio;
    private int mes;
    private double ingresos;
    private double gastos;
    private double saldo;
    private double saldoActualTotal;
    private double ingresosRecurrentes;
    private double balanceVisibleMes;
    private double balanceOperativoMes;
    private double initialCashBalance;
    private double initialCardBalance;
    private double efectivo;
    private double tarjetaCuenta;
    private double presupuestoMonto;
    private double presupuestoRestante;
    private double presupuestoPorcentaje;
    private boolean presupuestoExcedido;
    private double gastoProyectado;
    private double gastoPromedioDiario;
    private int diasRestantes;
    private int cantidadGastosMes;
    private int diasConMovimientoMes;
    private boolean proyeccionPreliminar;
    private String confianzaProyeccion;
    private String mensajeConfianzaProyeccion;
    private String riesgoPresupuesto;
    private double gastosMesAnterior;
    private double variacionGastosPorcentaje;
    private String categoriaMayorGasto;
    private double categoriaMayorGastoMonto;
    private String estadoFinanciero;
    private String insightPrincipal;
    private double proyeccionFinMes;
    private String alertaPrincipal;
    private int scoreFinanciero;
    private String scoreEstado;
    private String scoreExplicacion;
    private String scoreTendencia;
    private double ahorroSugerido;
    private String ahorroSugeridoMensaje;
    private String recomendacionAhorroMeta;
    private String estadoAhorro;
    private boolean proUser;
    private final List<CategoryBudgetSummary> presupuestosCategoria = new ArrayList<>();
    private final List<SavingsGoal> metas = new ArrayList<>();
    private final List<PaymentReminder> recordatorios = new ArrayList<>();
    private final List<String> alertas = new ArrayList<>();
    private final List<String> notasInformativas = new ArrayList<>();
    private final List<String> alertasPredictivas = new ArrayList<>();
    private final List<String> recomendacionesInteligentes = new ArrayList<>();
    private final List<CategoryChartSlice> chartCategorias = new ArrayList<>();
    private final List<MonthlyTrendPoint> tendenciaMensual = new ArrayList<>();
    private final List<AccountBalance> accountBalances = new ArrayList<>();
    private final List<Transaccion> latestTransactions = new ArrayList<>();
    private int importacionesPendientes;
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

    public double getSaldoActualTotal() { return saldoActualTotal; }
    public void setSaldoActualTotal(double saldoActualTotal) { this.saldoActualTotal = saldoActualTotal; }

    public double getIngresosRecurrentes() { return ingresosRecurrentes; }
    public void setIngresosRecurrentes(double ingresosRecurrentes) { this.ingresosRecurrentes = ingresosRecurrentes; }

    public double getBalanceVisibleMes() { return balanceVisibleMes; }
    public void setBalanceVisibleMes(double balanceVisibleMes) { this.balanceVisibleMes = balanceVisibleMes; }

    public double getBalanceOperativoMes() { return balanceOperativoMes; }
    public void setBalanceOperativoMes(double balanceOperativoMes) { this.balanceOperativoMes = balanceOperativoMes; }

    public double getInitialCashBalance() { return initialCashBalance; }
    public void setInitialCashBalance(double initialCashBalance) { this.initialCashBalance = initialCashBalance; }

    public double getInitialCardBalance() { return initialCardBalance; }
    public void setInitialCardBalance(double initialCardBalance) { this.initialCardBalance = initialCardBalance; }

    public double getSaldoInicialTotal() { return initialCashBalance + initialCardBalance; }

    public boolean hasInitialBalance() { return getSaldoInicialTotal() > 0.0; }

    public double getEfectivo() { return efectivo; }
    public void setEfectivo(double efectivo) { this.efectivo = efectivo; }

    public double getTarjetaCuenta() { return tarjetaCuenta; }
    public void setTarjetaCuenta(double tarjetaCuenta) { this.tarjetaCuenta = tarjetaCuenta; }

    public double getPresupuestoMonto() { return presupuestoMonto; }
    public void setPresupuestoMonto(double presupuestoMonto) { this.presupuestoMonto = presupuestoMonto; }

    public double getPresupuestoRestante() { return presupuestoRestante; }
    public void setPresupuestoRestante(double presupuestoRestante) { this.presupuestoRestante = presupuestoRestante; }

    public double getPresupuestoPorcentaje() { return presupuestoPorcentaje; }
    public void setPresupuestoPorcentaje(double presupuestoPorcentaje) { this.presupuestoPorcentaje = presupuestoPorcentaje; }

    public boolean isPresupuestoExcedido() { return presupuestoExcedido; }
    public boolean getPresupuestoExcedido() { return presupuestoExcedido; }
    public void setPresupuestoExcedido(boolean presupuestoExcedido) { this.presupuestoExcedido = presupuestoExcedido; }

    public double getGastoProyectado() { return gastoProyectado; }
    public void setGastoProyectado(double gastoProyectado) { this.gastoProyectado = gastoProyectado; }

    public double getGastoPromedioDiario() { return gastoPromedioDiario; }
    public void setGastoPromedioDiario(double gastoPromedioDiario) { this.gastoPromedioDiario = gastoPromedioDiario; }

    public int getDiasRestantes() { return diasRestantes; }
    public void setDiasRestantes(int diasRestantes) { this.diasRestantes = diasRestantes; }

    public int getCantidadGastosMes() { return cantidadGastosMes; }
    public void setCantidadGastosMes(int cantidadGastosMes) { this.cantidadGastosMes = cantidadGastosMes; }

    public int getDiasConMovimientoMes() { return diasConMovimientoMes; }
    public void setDiasConMovimientoMes(int diasConMovimientoMes) { this.diasConMovimientoMes = diasConMovimientoMes; }

    public boolean isProyeccionPreliminar() { return proyeccionPreliminar; }
    public boolean getProyeccionPreliminar() { return proyeccionPreliminar; }
    public void setProyeccionPreliminar(boolean proyeccionPreliminar) { this.proyeccionPreliminar = proyeccionPreliminar; }

    public String getConfianzaProyeccion() { return confianzaProyeccion; }
    public void setConfianzaProyeccion(String confianzaProyeccion) { this.confianzaProyeccion = confianzaProyeccion; }

    public String getMensajeConfianzaProyeccion() { return mensajeConfianzaProyeccion; }
    public void setMensajeConfianzaProyeccion(String mensajeConfianzaProyeccion) { this.mensajeConfianzaProyeccion = mensajeConfianzaProyeccion; }

    public String getRiesgoPresupuesto() { return riesgoPresupuesto; }
    public void setRiesgoPresupuesto(String riesgoPresupuesto) { this.riesgoPresupuesto = riesgoPresupuesto; }

    public double getGastosMesAnterior() { return gastosMesAnterior; }
    public void setGastosMesAnterior(double gastosMesAnterior) { this.gastosMesAnterior = gastosMesAnterior; }

    public double getVariacionGastosPorcentaje() { return variacionGastosPorcentaje; }
    public void setVariacionGastosPorcentaje(double variacionGastosPorcentaje) { this.variacionGastosPorcentaje = variacionGastosPorcentaje; }

    public String getCategoriaMayorGasto() { return categoriaMayorGasto; }
    public void setCategoriaMayorGasto(String categoriaMayorGasto) { this.categoriaMayorGasto = categoriaMayorGasto; }

    public double getCategoriaMayorGastoMonto() { return categoriaMayorGastoMonto; }
    public void setCategoriaMayorGastoMonto(double categoriaMayorGastoMonto) { this.categoriaMayorGastoMonto = categoriaMayorGastoMonto; }

    public String getEstadoFinanciero() { return estadoFinanciero; }
    public void setEstadoFinanciero(String estadoFinanciero) { this.estadoFinanciero = estadoFinanciero; }

    public String getInsightPrincipal() { return insightPrincipal; }
    public void setInsightPrincipal(String insightPrincipal) { this.insightPrincipal = insightPrincipal; }

    public double getProyeccionFinMes() { return proyeccionFinMes; }
    public void setProyeccionFinMes(double proyeccionFinMes) { this.proyeccionFinMes = proyeccionFinMes; }

    public String getAlertaPrincipal() { return alertaPrincipal; }
    public void setAlertaPrincipal(String alertaPrincipal) { this.alertaPrincipal = alertaPrincipal; }

    public int getScoreFinanciero() { return scoreFinanciero; }
    public void setScoreFinanciero(int scoreFinanciero) { this.scoreFinanciero = scoreFinanciero; }

    public String getScoreEstado() { return scoreEstado; }
    public void setScoreEstado(String scoreEstado) { this.scoreEstado = scoreEstado; }

    public String getScoreExplicacion() { return scoreExplicacion; }
    public void setScoreExplicacion(String scoreExplicacion) { this.scoreExplicacion = scoreExplicacion; }

    public String getScoreTendencia() { return scoreTendencia; }
    public void setScoreTendencia(String scoreTendencia) { this.scoreTendencia = scoreTendencia; }

    public double getAhorroSugerido() { return ahorroSugerido; }
    public void setAhorroSugerido(double ahorroSugerido) { this.ahorroSugerido = ahorroSugerido; }

    public String getAhorroSugeridoMensaje() { return ahorroSugeridoMensaje; }
    public void setAhorroSugeridoMensaje(String ahorroSugeridoMensaje) { this.ahorroSugeridoMensaje = ahorroSugeridoMensaje; }

    public String getRecomendacionAhorroMeta() { return recomendacionAhorroMeta; }
    public void setRecomendacionAhorroMeta(String recomendacionAhorroMeta) { this.recomendacionAhorroMeta = recomendacionAhorroMeta; }

    public String getEstadoAhorro() { return estadoAhorro; }
    public void setEstadoAhorro(String estadoAhorro) { this.estadoAhorro = estadoAhorro; }

    public boolean isProUser() { return proUser; }
    public boolean getProUser() { return proUser; }
    public void setProUser(boolean proUser) { this.proUser = proUser; }

    public List<CategoryBudgetSummary> getPresupuestosCategoria() { return presupuestosCategoria; }

    public List<SavingsGoal> getMetas() { return metas; }

    public List<PaymentReminder> getRecordatorios() { return recordatorios; }

    public List<String> getAlertas() { return alertas; }

    public List<String> getNotasInformativas() { return notasInformativas; }

    public List<String> getAlertasPredictivas() { return alertasPredictivas; }

    public List<String> getRecomendacionesInteligentes() { return recomendacionesInteligentes; }

    public List<CategoryChartSlice> getChartCategorias() { return chartCategorias; }

    public List<MonthlyTrendPoint> getTendenciaMensual() { return tendenciaMensual; }

    public List<AccountBalance> getAccountBalances() { return accountBalances; }

    public List<Transaccion> getLatestTransactions() { return latestTransactions; }

    public int getImportacionesPendientes() { return importacionesPendientes; }
    public void setImportacionesPendientes(int importacionesPendientes) { this.importacionesPendientes = importacionesPendientes; }

    public List<ExchangeRate> getTiposCambio() { return tiposCambio; }

    public ConversionSummary getConversion() { return conversion; }
    public void setConversion(ConversionSummary conversion) { this.conversion = conversion; }

    public List<DashboardModulePref> getDashboardPreferencias() { return dashboardPreferencias; }

    public TravelPreference getTravelPreference() { return travelPreference; }
    public void setTravelPreference(TravelPreference travelPreference) { this.travelPreference = travelPreference; }

    public List<GamificationChallenge> getGamificacionRetos() { return gamificacionRetos; }
}
