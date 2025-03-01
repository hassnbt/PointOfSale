package models;

import java.time.LocalDateTime;

public class ProductSalesSummary {
    private long id;
    private String name;
    private double soldQuantity;
    private double soldTotal;
    private double soldPerUnitQuantity;
    private double factoryQuantity;
    private double factoryTotal;
    private double factorLooseCost;
    private double quantityPerUnit;

    public ProductSalesSummary(long id, String name, double soldQuantity, double soldTotal,
                               double soldPerUnitQuantity, double factoryQuantity,
                               double factoryTotal, double factorLooseCost, double quantityPerUnit) {
        this.id = id;
        this.name = name;
        this.soldQuantity = soldQuantity;
        this.soldTotal = soldTotal;
        this.soldPerUnitQuantity = soldPerUnitQuantity;
        this.factoryQuantity = factoryQuantity;
        this.factoryTotal = factoryTotal;
        this.factorLooseCost = factorLooseCost;
        this.quantityPerUnit = quantityPerUnit;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public double getSoldQuantity() { return soldQuantity; }
    public double getSoldTotal() { return soldTotal; }
    public double getSoldPerUnitQuantity() { return soldPerUnitQuantity; }
    public double getFactoryQuantity() { return factoryQuantity; }
    public double getFactoryTotal() { return factoryTotal; }
    public double getFactorLooseCost() { return factorLooseCost; }
    public double getQuantityPerUnit() { return quantityPerUnit; }
}
