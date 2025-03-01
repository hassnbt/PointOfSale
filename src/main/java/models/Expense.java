package models;

import java.time.LocalDateTime;

public class Expense {
    private long id;
    private String notes;
    private double amount;

    private LocalDateTime createdOn;
    private boolean active;

    public Expense(long id, String notes, double amount, LocalDateTime createdOn, boolean active) {
        this.id = id;
        this.notes = notes;
        this.amount = amount;

        this.createdOn = createdOn;
        this.active = active;
    }

    public long getId() { return id; }
    public String getNotes() { return notes; }
    public double getAmount() { return amount; }

    public LocalDateTime getCreatedOn() { return createdOn; }
    public boolean isActive() { return active; }
}
