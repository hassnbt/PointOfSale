package models;

import java.time.LocalDateTime;

public class Product {
    private int id;
    private String name;
    private double price;
    private int quantity;
    private LocalDateTime createdOn;
    private String createdBy;
    private boolean isActive;

    // Constructor
    public Product(int id, String name, double price, int quantity, LocalDateTime createdOn, String createdBy, boolean isActive) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.createdOn = createdOn;
        this.createdBy = createdBy;
        this.isActive = isActive;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public LocalDateTime getCreatedOn() { return createdOn; }
    public String getCreatedBy() { return createdBy; }
    public boolean isActive() { return isActive; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setCreatedOn(LocalDateTime createdOn) { this.createdOn = createdOn; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", createdOn=" + createdOn +
                ", createdBy='" + createdBy + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
