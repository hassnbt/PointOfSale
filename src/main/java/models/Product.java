package models;

import java.time.LocalDateTime;

public class Product {
    private int id;
    private String name;
    private double price;
    private int quantity;
    private int quantityPerUnit;   // renamed field
    private LocalDateTime createdOn;
    private String createdBy;
    private boolean isActive;
    private double originalPrice;  // renamed field

    // Constructor with default quantityPerUnit
    public Product(int id, String name, double price, int quantity, LocalDateTime createdOn, String createdBy, boolean isActive) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.createdOn = createdOn;
        this.createdBy = createdBy;
        this.isActive = isActive;
        this.quantityPerUnit = 6;
    }

    // Full constructor
    public Product(int id, String name, double price, int quantity, LocalDateTime createdOn, String createdBy, boolean isActive, int quantityPerUnit, double originalPrice) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.createdOn = createdOn;
        this.createdBy = createdBy;
        this.isActive = isActive;
        this.quantityPerUnit = quantityPerUnit;
        this.originalPrice = originalPrice;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public LocalDateTime getCreatedOn() { return createdOn; }
    public String getCreatedBy() { return createdBy; }
    public boolean isActive() { return isActive; }
    public int getQuantityPerUnit() { return quantityPerUnit; }  // corrected getter name
    public double getOriginalPrice() { return originalPrice; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setCreatedOn(LocalDateTime createdOn) { this.createdOn = createdOn; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setActive(boolean active) { isActive = active; }
    public void setQuantityPerUnit(int quantityPerUnit) { this.quantityPerUnit = quantityPerUnit; }  // corrected setter name
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }

    @Override
    public String toString() {
        return name;
    }
}
