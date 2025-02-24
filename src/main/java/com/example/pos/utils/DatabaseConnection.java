package com.example.pos.utils;



import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    static final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
    static final String USER = "sysdba";
    static final String PASSWORD = "123456";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
