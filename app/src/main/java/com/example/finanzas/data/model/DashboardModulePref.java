package com.example.finanzas.data.model;

public class DashboardModulePref {
    private String id;
    private boolean visible = true;

    public DashboardModulePref() { }

    public DashboardModulePref(String id, boolean visible) {
        this.id = id;
        this.visible = visible;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
}
