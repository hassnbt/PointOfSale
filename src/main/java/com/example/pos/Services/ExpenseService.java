package com.example.pos.Services;

import models.Expense;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExpenseService {

    private static final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
    private static final String USER = "sysdba";
    private static final String PASSWORD = "123456";

    // Retrieve all expense records
    public List<Expense> getAllExpenses() throws SQLException {
        List<Expense> list = new ArrayList<>();
        String sql = "SELECT ID, NOTES, AMOUNT ,CREATED_ON, IS_ACTIVE FROM EXPENSE";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                long id = rs.getLong("ID");
                String notes = rs.getString("NOTES");
                double amount = rs.getDouble("AMOUNT");

                Timestamp ts = rs.getTimestamp("CREATED_ON");
                LocalDateTime createdOn = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                boolean active = rs.getBoolean("IS_ACTIVE");
                list.add(new Expense(id, notes, amount, createdOn, active));
            }
        }
        return list;
    }

    // Insert a new expense record with two amount fields
    public void addExpense(String note, double amount) throws SQLException {
        String sql = "INSERT INTO EXPENSE ( NOTES, AMOUNT, CREATED_ON, IS_ACTIVE) VALUES ( ?, ?, CURRENT_TIMESTAMP, ?)";
        long id = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, note);
            pstmt.setDouble(2, amount);

            pstmt.setBoolean(3, true);
            pstmt.executeUpdate();
        }
    }
}
