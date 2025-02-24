package com.example.pos.Services;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.*;

import javafx.scene.image.Image;

public class ImageService {

    // Update these with your Firebird connection details
    private static final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";

    private static final String USER = "sysdba";
    private static final String PASSWORD = "123456";

    /**
     * Saves the given image file with the provided name into the database.
     */
    public void saveImage(String name, File imageFile) throws Exception {
        String sql = "INSERT INTO IMAGES (ID, NAME, IMAGE_DATA) VALUES (?, ?, ?)";
        // For simplicity, using current time in millis as an ID (ensure uniqueness)
        long id = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.setString(2, name);
            try (FileInputStream fis = new FileInputStream(imageFile)) {
                pstmt.setBinaryStream(3, fis, (int) imageFile.length());
                pstmt.executeUpdate();
            }
        }
    }

    /**
     * Loads an image from the database by the given image ID.
     * Returns a JavaFX Image or null if not found.
     */
    public Image loadImage(long id) throws Exception {
        String sql = "SELECT IMAGE_DATA FROM IMAGES WHERE ID = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    InputStream is = rs.getBinaryStream("IMAGE_DATA");
                    if (is != null) {
                        return new Image(is);
                    }
                }
            }
        }
        return null;
    }
}
