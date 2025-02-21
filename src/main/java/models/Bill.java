package models;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Bill {
    private long billid;
    private double cashIn;
    private double cashOut;
    private LocalDateTime created;

    private boolean isActive;
    private String name;
    private String note;
    private double total;
    private double discount;
    private LocalDateTime updated;
private long areaid;
    public Bill(long billid, double cashIn, double cashOut, LocalDateTime created, boolean isActive, String name, String note, double total, double discount) {
        this.billid = billid;
        this.cashIn = cashIn;
        this.cashOut = cashOut;
        this.created = created;
        this.isActive = isActive;
        this.name = name;
        this.note = note;
        this.total = total;
        this.discount = discount;

    }
    public Bill(long billid, double cashIn, double cashOut, LocalDateTime created, boolean isActive, String name, String note, double total, double discount,String showdate) {
        this.billid = billid;
        this.cashIn = cashIn;
        this.cashOut = cashOut;
        this.created = created;
        this.isActive = isActive;
        this.name = name;
        this.note = note;
        this.total = total;
        this.discount = discount;

    }
    public Bill(long billid, double cashIn, double cashOut, LocalDateTime created, boolean isActive, String name, String note, double total, double discount,String showdate,long areaid) {
        this.billid = billid;
        this.cashIn = cashIn;
        this.cashOut = cashOut;
        this.created = created;
        this.isActive = isActive;
        this.name = name;
        this.note = note;
        this.total = total;
        this.discount = discount;
        this.areaid=areaid;

    }

    // Getters and setters
    public long getBillid() { return billid; }
    public void setBillid(long billid) { this.billid = billid; }
    public double getCashIn() { return cashIn; }
    public void setCashIn(double cashIn) { this.cashIn = cashIn; }
    public double getCashOut() { return cashOut; }
    public void setCashOut(double cashOut) { this.cashOut = cashOut; }
    public LocalDateTime getCreated() { return created; }
    public void setCreated(LocalDateTime created) { this.created = created; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public String getShowdate() {
       // LocalDateTime created = ts.toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss");
        return created.format(formatter);
    }
    public void setShowdate(String showdate) {
    }

    public String getUpdated() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss");
        return updated.format(formatter);
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public long getAreaid() {
        return areaid;
    }

    public void setAreaid(long areaid) {
        this.areaid = areaid;
    }
}
