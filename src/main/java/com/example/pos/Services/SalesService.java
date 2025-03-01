package com.example.pos.Services;


import models.ProductSalesSummary;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SalesService {

    private static final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
    private static final String USER = "sysdba";
    private static final String PASSWORD = "123456";

    public List<ProductSalesSummary> getProductSalesSummary(LocalDateTime start, LocalDateTime end) throws SQLException {
        List<ProductSalesSummary> list = new ArrayList<>();

        String sql = "SELECT \n" +
                "    p.ID,\n" +
                "    p.\"NAME\",\n" +
                "    COALESCE(c.sold_qty+(c.qun/p.QUANTITY_PERUNIT), 0) AS sold_quantity,\n" +
                "    COALESCE(c.sold_total+c.sold_loose_cost, 0) AS sold_total,\n" +
                "    COALESCE(c.sold_loose_cost, 0) AS sold_perunitquantity,\n" +
                "    COALESCE(vc.factor_qty, 0) AS Factory_Quantity,\n" +
                "    COALESCE(vc.factor_total, 0) AS Factory_Total,\n" +
                "    COALESCE(vc.factor_loose_cost, 0) AS factor_loose_cost,\n" +
                "    COALESCE(MOD(c.qun, p.QUANTITY_PERUNIT), 0) AS Quantity_per_unit\n" +
                "FROM PRODUCTS p\n" +
                "LEFT JOIN (\n" +
                "    SELECT \n" +
                "        c.PRODUCT_ID,\n" +
                "        SUM(c.QUANTITY) AS sold_qty,\n" +
                "        SUM(c.PRICE * c.QUANTITY) AS sold_total,\n" +
                "        SUM((c.PRICE / p_inner.QUANTITY_PERUNIT) * c.QUANTITY_PER_UNIT) AS sold_loose_cost,\n" +
                "        SUM(c.QUANTITY_PER_UNIT / p_inner.QUANTITY_PERUNIT) AS addinamount,\n" +
                "        SUM(c.QUANTITY_PER_UNIT) AS qun\n" +
                "    FROM CART c\n" +
                "    JOIN PRODUCTS p_inner ON c.PRODUCT_ID = p_inner.ID\n" +
                "    WHERE c.CREATED_ON BETWEEN ? AND ?\n" +
                "      AND c.IS_ACTIVE = TRUE\n" +
                "    GROUP BY c.PRODUCT_ID\n" +
                ") c ON p.ID = c.PRODUCT_ID\n" +
                "LEFT JOIN (\n" +
                "    SELECT \n" +
                "        PRODUCT_ID,\n" +
                "        SUM(QUANTITY) AS factor_qty,\n" +
                "        SUM(PRICE * QUANTITY) AS factor_total,\n" +
                "        SUM(PRICE * QUANTITY_PER_UNIT) AS factor_loose_cost\n" +
                "    FROM VENDOR_CART\n" +
                "    WHERE CREATED_ON BETWEEN ? AND ?\n" +
                "    GROUP BY PRODUCT_ID\n" +
                ") vc ON p.ID = vc.PRODUCT_ID";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(start));
            pstmt.setTimestamp(2, Timestamp.valueOf(end));
            pstmt.setTimestamp(3, Timestamp.valueOf(start));
            pstmt.setTimestamp(4, Timestamp.valueOf(end));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("ID");
                    String name = rs.getString("NAME");
                    double soldQuantity = rs.getDouble("sold_quantity");
                    double soldTotal = rs.getDouble("sold_total");
                    double soldPerUnitQuantity = rs.getDouble("sold_perunitquantity");
                    double factoryQuantity = rs.getDouble("Factory_Quantity");
                    double factoryTotal = rs.getDouble("Factory_Total");
                    double factorLooseCost = rs.getDouble("factor_loose_cost");
                    double quantityPerUnit = rs.getDouble("Quantity_per_unit");

                    ProductSalesSummary summary = new ProductSalesSummary(
                            id, name, soldQuantity, soldTotal, soldPerUnitQuantity,
                            factoryQuantity, factoryTotal, factorLooseCost, quantityPerUnit
                    );
                    list.add(summary);
                }
            }
        }
        return list;
    }
    public List<ProductSalesSummary> getProductSalesSummaryAll() throws SQLException {
        List<ProductSalesSummary> list = new ArrayList<>();

        String sql = "SELECT \n" +
                "    p.ID,\n" +
                "    p.\"NAME\",\n" +
                "    COALESCE(c.sold_qty + (c.qun / p.QUANTITY_PERUNIT), 0) AS sold_quantity,\n" +
                "    COALESCE(c.sold_total + c.sold_loose_cost, 0) AS sold_total,\n" +
                "    COALESCE(c.sold_loose_cost, 0) AS sold_perunitquantity,\n" +
                "    COALESCE(vc.factor_qty, 0) AS Factory_Quantity,\n" +
                "    COALESCE(vc.factor_total, 0) AS Factory_Total,\n" +
                "    COALESCE(vc.factor_loose_cost, 0) AS factor_loose_cost,\n" +
                "    COALESCE(MOD(c.qun, p.QUANTITY_PERUNIT), 0) AS Quantity_per_unit\n" +
                "FROM PRODUCTS p\n" +
                "LEFT JOIN (\n" +
                "    SELECT \n" +
                "        c.PRODUCT_ID,\n" +
                "        SUM(c.QUANTITY) AS sold_qty,\n" +
                "        SUM(c.PRICE * c.QUANTITY) AS sold_total,\n" +
                "        SUM((c.PRICE / p_inner.QUANTITY_PERUNIT) * c.QUANTITY_PER_UNIT) AS sold_loose_cost,\n" +
                "        SUM(c.QUANTITY_PER_UNIT) AS qun\n" +
                "    FROM CART c\n" +
                "    JOIN PRODUCTS p_inner ON c.PRODUCT_ID = p_inner.ID\n" +
                "    WHERE c.IS_ACTIVE = TRUE\n" +
                "    GROUP BY c.PRODUCT_ID\n" +
                ") c ON p.ID = c.PRODUCT_ID\n" +
                "LEFT JOIN (\n" +
                "    SELECT \n" +
                "        PRODUCT_ID,\n" +
                "        SUM(QUANTITY) AS factor_qty,\n" +
                "        SUM(PRICE * QUANTITY) AS factor_total,\n" +
                "        SUM(PRICE * QUANTITY_PER_UNIT) AS factor_loose_cost\n" +
                "    FROM VENDOR_CART\n" +
                "    GROUP BY PRODUCT_ID\n" +
                ") vc ON p.ID = vc.PRODUCT_ID";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("ID");
                    String name = rs.getString("NAME");
                    double soldQuantity = rs.getDouble("sold_quantity");
                    double soldTotal = rs.getDouble("sold_total");
                    double soldPerunitquantity = rs.getDouble("sold_perunitquantity");
                    double factoryQuantity = rs.getDouble("Factory_Quantity");
                    double factoryTotal = rs.getDouble("Factory_Total");
                    double factorLooseCost = rs.getDouble("factor_loose_cost");
                    double quantityPerUnit = rs.getDouble("Quantity_per_unit");

                    ProductSalesSummary summary = new ProductSalesSummary(
                            id, name, soldQuantity, soldTotal, soldPerunitquantity,
                            factoryQuantity, factoryTotal, factorLooseCost, quantityPerUnit
                    );
                    list.add(summary);
                }
            }
        }
        return list;
    }
}
