package com.example.pos;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Product;

import java.io.IOException;

import models.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
public class HomeController {



    private void switchScene(ActionEvent event, String fxmlFile, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, 800, 600);

        // Apply Fade Transition
        root.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        stage.setScene(scene);
        stage.setTitle(title);
        stage.setMaximized(true); // Keep window maximized
        stage.show();
    }


    @FXML
    private void handleBackButton(ActionEvent event) throws IOException {
        switchScene(event, "hello-view.fxml", "Dosa Cola POS System");
    }

    @FXML
    private void handleInventoryButton(ActionEvent event) throws IOException {
        switchScene(event, "inventory.fxml", "Inventory Management");
    }

    @FXML
    private void handleSalesButton(ActionEvent event) throws IOException {
        switchScene(event, "sales.fxml", "Sales");
    }

    @FXML
    private void handleReportsButton(ActionEvent event) throws IOException {
        switchScene(event, "reports.fxml", "Reports");
    }

    @FXML private TextField searchField;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> productNameColumn;
    @FXML private TableColumn<Product, Double> priceColumn;
    @FXML private TableColumn<Product, Integer> quantityColumn;
    @FXML private TableColumn<Product, Double> totalColumn;
    @FXML private Label totalAmountLabel;

    private ObservableList<Product> cart = FXCollections.observableArrayList();
    private Map<String, Product> availableProducts = new HashMap<>();

    @FXML
    public void initialize() {
        // Initialize table columns
        searchField.textProperty().addListener((observable, oldValue, newValue) -> handleSearch());
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        totalColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getPrice() * cellData.getValue().getQuantity()).asObject()
        );

        productTable.setItems(cart);

        // Sample product list (Replace this with database fetching logic)
        availableProducts.put("Apple", new Product(1, "Apple", 1.50, 50, LocalDateTime.now(), "Admin", true));
        availableProducts.put("Banana", new Product(2, "Banana", 0.80, 30, LocalDateTime.now(), "Admin", true));
        availableProducts.put("Milk", new Product(3, "Milk", 2.30, 20, LocalDateTime.now(), "Admin", true));
        availableProducts.put("Bread", new Product(4, "Bread", 1.20, 25, LocalDateTime.now(), "Admin", true));
    }
    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim().toLowerCase();

        if (query.isEmpty()) {
            productTable.setItems(cart); // Show all products in cart if search is empty
            return;
        }

        ObservableList<Product> filteredProducts = FXCollections.observableArrayList();

        for (Product product : availableProducts.values()) {
            if (product.getName().toLowerCase().contains(query)) {
                filteredProducts.add(product);
            }
        }

        productTable.setItems(filteredProducts);
    }

    @FXML
    private void handleAddProduct(ActionEvent event) {
        String productName = searchField.getText().trim();

        if (productName.isEmpty()) {
            showAlert("Error", "Please enter a product name!", Alert.AlertType.ERROR);
            return;
        }

        if (availableProducts.containsKey(productName)) {
            Product selectedProduct = availableProducts.get(productName);

            // Check if the product is already in cart
            Product cartProduct = findProductInCart(productName);
            if (cartProduct != null) {
                cartProduct.setQuantity(cartProduct.getQuantity() + 1);
            } else {
                cart.add(new Product(selectedProduct.getId(), selectedProduct.getName(), selectedProduct.getPrice(), 1, LocalDateTime.now(), "User", true));
            }

            productTable.refresh();
            updateTotalAmount();
        } else {
            showAlert("Not Found", "Product not available!", Alert.AlertType.WARNING);
        }

        searchField.clear();
    }

    private Product findProductInCart(String productName) {
        return cart.stream().filter(p -> p.getName().equalsIgnoreCase(productName)).findFirst().orElse(null);
    }

    private void updateTotalAmount() {
        double total = cart.stream().mapToDouble(p -> p.getPrice() * p.getQuantity()).sum();
        totalAmountLabel.setText(String.format("$%.2f", total));
    }

    @FXML
    private void handleClear(ActionEvent event) {
        cart.clear();
        updateTotalAmount();
    }

    @FXML
    private void handleCheckout(ActionEvent event) {
        if (cart.isEmpty()) {
            showAlert("Error", "Cart is empty!", Alert.AlertType.ERROR);
            return;
        }

        showAlert("Checkout Successful", "Total Amount: " + totalAmountLabel.getText(), Alert.AlertType.INFORMATION);
        cart.clear();
        updateTotalAmount();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
