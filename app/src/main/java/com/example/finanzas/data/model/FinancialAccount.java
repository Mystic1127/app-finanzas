package com.example.finanzas.data.model;

public class FinancialAccount {
    private String id;
    private String name;
    private long createdAt;
    private String last4;
    private boolean includedInTotal;
    private boolean visibleInHome;
    private boolean userAdded;

    public FinancialAccount() {
    }

    public FinancialAccount(String id, String name, long createdAt) {
        this(id, name, createdAt, "4242", true, false, false);
    }

    public FinancialAccount(String id, String name, long createdAt, String last4, boolean includedInTotal, boolean visibleInHome, boolean userAdded) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.last4 = last4;
        this.includedInTotal = includedInTotal;
        this.visibleInHome = visibleInHome;
        this.userAdded = userAdded;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getLast4() {
        return last4;
    }

    public void setLast4(String last4) {
        this.last4 = last4;
    }

    public boolean isIncludedInTotal() {
        return includedInTotal;
    }

    public boolean getIncludedInTotal() {
        return includedInTotal;
    }

    public void setIncludedInTotal(boolean includedInTotal) {
        this.includedInTotal = includedInTotal;
    }

    public boolean isVisibleInHome() {
        return visibleInHome;
    }

    public boolean getVisibleInHome() {
        return visibleInHome;
    }

    public void setVisibleInHome(boolean visibleInHome) {
        this.visibleInHome = visibleInHome;
    }

    public boolean isUserAdded() {
        return userAdded;
    }

    public boolean getUserAdded() {
        return userAdded;
    }

    public void setUserAdded(boolean userAdded) {
        this.userAdded = userAdded;
    }
}
