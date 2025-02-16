package com.example.pos;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import models.Product;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;

public class HelloController {

    public Button inventoryButton;
    @FXML
    private TableView<Product> productTable;
    @FXML
    private TableColumn<Product, String> productColumn;
    @FXML
    private TableColumn<Product, Double> priceColumn;
    @FXML
    private TableColumn<Product, Integer> quantityColumn;
    @FXML
    private Button addButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button increaseQuantityButton;
    @FXML
    private Button decreaseQuantityButton;
    @FXML
    private TextField productNameField;
    @FXML
    private TextField priceField;
    @FXML
    private TextField quantityField;
    @FXML
    private TextField searchField;

    private ObservableList<Product> products = FXCollections.observableArrayList();
    private FilteredList<Product> filteredProducts;

    @FXML
    public void initialize() {
        productColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/MYDATABASE.fdb";
        final String USER = "sysdba";
        final String PASSWORD = "123456";
        // Dummy product data
        String query = "SELECT ID, NAME, PRICE, QUANTITY, CREATED_AT, CREATED_BY, IS_AVAILABLE FROM PRODUCTS";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("ID");
                String name = rs.getString("NAME");
                double price = rs.getDouble("PRICE");
                int quantity = rs.getInt("QUANTITY");
                LocalDateTime createdAt = rs.getTimestamp("CREATED_AT").toLocalDateTime();
                String createdBy = rs.getString("CREATED_BY");
                boolean active = rs.getBoolean("IS_AVAILABLE");

                products.add(new Product(id, name, price, quantity, createdAt, createdBy, active));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Filtered list for search functionality
        filteredProducts = new FilteredList<>(products, p -> true);
        productTable.setItems(filteredProducts);

        // Bind search input to product filtering
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredProducts.setPredicate(product -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true; // Show all products when search is empty
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return product.getName().toLowerCase().contains(lowerCaseFilter);
            });
        });
    }

    @FXML
    private void handleAddProduct() {
        try {
            String name = productNameField.getText();
            double price = Double.parseDouble(priceField.getText());
            int quantity = Integer.parseInt(quantityField.getText());

            Product newProduct = new Product(products.size() + 1, name, price, quantity, LocalDateTime.now(), "Admin", true);
            products.add(newProduct);

            clearFields();
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter valid values for price and quantity.");
        }
    }

    @FXML
    private void handleDeleteProduct() {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            products.remove(selectedProduct);
        } else {
            showAlert("No Selection", "Please select a product to delete.");
        }
    }

    @FXML
    private void handleIncreaseQuantity() {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            selectedProduct.setQuantity(selectedProduct.getQuantity() + 1);
            productTable.refresh();
        } else {
            showAlert("No Selection", "Please select a product to increase quantity.");
        }
    }

    @FXML
    private void handleDecreaseQuantity() {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct != null && selectedProduct.getQuantity() > 0) {
            selectedProduct.setQuantity(selectedProduct.getQuantity() - 1);
            productTable.refresh();
        } else {
            showAlert("No Selection", "Please select a product to decrease quantity.");
        }
    }

    private void clearFields() {
        productNameField.clear();
        priceField.clear();
        quantityField.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    private void handleHomeButton(ActionEvent event) throws IOException {
        // Load the Home.fxml file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("home.fxml"));
        Parent homeRoot = loader.load();

        // Get the current stage
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        // Apply Fade Transition
        homeRoot.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), homeRoot);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Set the new scene while keeping full-screen mode
        Scene homeScene = new Scene(homeRoot, 800, 2500);
        stage.setScene(homeScene);
        stage.setTitle("Home Screen");
        stage.setMaximized(true);  // Ensures full-screen mode remains
        stage.show();
    }

}
