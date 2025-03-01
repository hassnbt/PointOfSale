package models;

import java.time.LocalDateTime;

public class AmountReceiveRecord {
    private long id;
    private long billId;
    private String note;
    private LocalDateTime createdOn;
    private boolean active;
    private double amount;

    public AmountReceiveRecord(long id, long billId, String note, LocalDateTime createdOn, boolean active, double amount) {
        this.id = id;
        this.billId = billId;
        this.note = note;
        this.createdOn = createdOn;
        this.active = active;
        this.amount = amount;
    }

    public long getId() {
        return id;
    }

    public long getBillId() {
        return billId;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public boolean isActive() {
        return active;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
