package models;

import java.time.LocalDateTime;

public class vendorcart {
    private long vid;
    private long billId;
    private long productId;
    private String name;
    private double price;
    private int quantity;
    private double originalPrice;
    private LocalDateTime createdOn;
    private String createdBy;
    private boolean isActive;
    private int quantityPerUnit;

    public vendorcart(long vid, long billId, long productId, String name, double price, int quantity,
                      double originalPrice, LocalDateTime createdOn, String createdBy, boolean isActive, int quantityPerUnit) {
        this.vid = vid;
        this.billId = billId;
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.originalPrice = originalPrice;
        this.createdOn = createdOn;
        this.createdBy = createdBy;
        this.isActive = isActive;
        this.quantityPerUnit = quantityPerUnit;
    }

    // Getters and Setters
    public long getVid() {
        return vid;
    }

    public void setVid(long vid) {
        this.vid = vid;
    }

    public long getBillId() {
        return billId;
    }

    public void setBillId(long billId) {
        this.billId = billId;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getQuantityPerUnit() {
        return quantityPerUnit;
    }

    public void setQuantityPerUnit(int quantityPerUnit) {
        this.quantityPerUnit = quantityPerUnit;
    }

    @Override
    public String toString() {
        return "VendorCart{" +
                "vid=" + vid +
                ", billId=" + billId +
                ", productId=" + productId +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", originalPrice=" + originalPrice +
                ", createdOn=" + createdOn +
                ", createdBy='" + createdBy + '\'' +
                ", isActive=" + isActive +
                ", quantityPerUnit=" + quantityPerUnit +
                '}';
    }
}
