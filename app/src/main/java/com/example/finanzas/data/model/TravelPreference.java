package com.example.finanzas.data.model;

public class TravelPreference {
    private boolean enabled;
    private String base;
    private String currency;
    private double rate;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getBase() { return base; }
    public void setBase(String base) { this.base = base; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public double getRate() { return rate; }
    public void setRate(double rate) { this.rate = rate; }
}
