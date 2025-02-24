package models;

import java.time.LocalDateTime;

public class vendorbill {
    private long bid;
    private long fvid;
    private double amountReceived;
    private double amountPending;
    private double discount;
    private double Total;
    private LocalDateTime createdOn;
    private boolean active;

    public vendorbill(long bid, long fvid, double amountReceived, double amountPending, double discount,
                      double amountPendingTotal, LocalDateTime createdOn, boolean active) {
        this.bid = bid;
        this.fvid = fvid;
        this.amountReceived = amountReceived;
        this.amountPending = amountPending;
        this.discount = discount;
        this.Total = amountPendingTotal;
        this.createdOn = createdOn;
        this.active = active;
    }

    // Getters and Setters
    public long getBid() {
        return bid;
    }

    public void setBid(long bid) {
        this.bid = bid;
    }

    public long getFvid() {
        return fvid;
    }

    public void setFvid(long fvid) {
        this.fvid = fvid;
    }

    public double getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(double amountReceived) {
        this.amountReceived = amountReceived;
    }

    public double getAmountPending() {
        return amountPending;
    }

    public void setAmountPending(double amountPending) {
        this.amountPending = amountPending;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getTotal() {
        return Total;
    }

    public void setTotal(double amountPendingTotal) {
        this.Total = amountPendingTotal;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "VendorBill{" +
                "bid=" + bid +
                ", fvid=" + fvid +
                ", amountReceived=" + amountReceived +
                ", amountPending=" + amountPending +
                ", discount=" + discount +
                ", amountPendingTotal=" +Total +
                ", createdOn=" + createdOn +
                ", isActive=" + active +
                '}';
    }
}
