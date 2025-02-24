package com.example.pos;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Vendor;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class VendorController {

    @FXML private TableView<Vendor> vendorTable;
    @FXML private TableColumn<Vendor, Long> vidColumn;
    @FXML private TableColumn<Vendor, String> vendorNameColumn;
    @FXML private TableColumn<Vendor, String> phoneNoColumn;
    @FXML private TableColumn<Vendor, String> addressColumn;
    @FXML private TableColumn<Vendor, LocalDateTime> createdOnColumn;
    @FXML private TableColumn<Vendor, Boolean> activeColumn;

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private CheckBox activeCheckBox;
    @FXML private Button addVendorButton;

    private ObservableList<Vendor> vendorList = FXCollections.observableArrayList();

    // Firebird DB connection info
    private static final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
    private static final String USER = "sysdba";
    private static final String PASSWORD = "123456";

    // Formatter for created_on date
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML
    public void initialize() {
        setupTableColumns();
        loadVendors();
    }

    private void setupTableColumns() {
        vidColumn.setCellValueFactory(new PropertyValueFactory<>("vid"));
        vendorNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        phoneNoColumn.setCellValueFactory(new PropertyValueFactory<>("phoneNo"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        createdOnColumn.setCellValueFactory(new PropertyValueFactory<>("createdOn"));
        // Format the created_on date in a friendly format.
        createdOnColumn.setCellFactory(column -> new TableCell<Vendor, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(dateFormatter));
                }
            }
        });
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
    }

    private void loadVendors() {
        vendorList.clear();
        String sql = "SELECT * FROM vendor";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                long vid = rs.getLong("vid");
                String name = rs.getString("name");
                String phone = rs.getString("phone_no");
                String address = rs.getString("address");
                Timestamp ts = rs.getTimestamp("created_on");
                LocalDateTime createdOn = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();
                boolean isActive = rs.getBoolean("is_active");
                Vendor vendor = new Vendor(vid, name, phone, address, createdOn, isActive);
                vendorList.add(vendor);
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Error loading vendors: " + e.getMessage());
        }
        vendorTable.setItems(vendorList);
    }

    @FXML

    private void handleAddVendor(ActionEvent event) {
        try {
            String name = nameField.getText();
            String phone = phoneField.getText();
            String address = addressField.getText();
            boolean isActive = activeCheckBox.isSelected();

            // Check if both name and phone are provided.
            if (name == null || name.trim().isEmpty() ||
                    phone == null || phone.trim().isEmpty()) {
                showAlert("Input Error", "Name and Phone Number are required.");
                return;
            }

            // Insert new vendor; created_on is set to CURRENT_TIMESTAMP.
            String sql = "INSERT INTO vendor (name, phone_no, address, created_on, is_active) VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?)";
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, name);
                pstmt.setString(2, phone);
                pstmt.setString(3, address);
                pstmt.setBoolean(4, isActive);
                pstmt.executeUpdate();
            }
            loadVendors();
            clearFields();
        } catch (SQLException e) {
            showAlert("Database Error", "Error adding vendor: " + e.getMessage());
        }
    }

    private void clearFields() {
        nameField.clear();
        phoneField.clear();
        addressField.clear();
        activeCheckBox.setSelected(true);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Optional: Scene switching method if you need to navigate from the vendor page.
//    private void switchScene(ActionEvent event, String fxmlFile, String title) throws IOException {
//        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
//        Parent root = loader.load();
//        Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
//        Scene scene = new Scene(root);
//        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
//        fadeIn.setFromValue(0);
//        fadeIn.setToValue(1);
//        fadeIn.play();
//        stage.setScene(scene);
//        stage.setTitle(title);
//        stage.show();
//    }
}
