package com.example.pos;

import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Product;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class HomeController {

    // Scene switching method (as before)
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
        stage.setMaximized(true);
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

    // Center pane elements (search and add)
    @FXML private TextField searchField;
    @FXML private TextField quantityField;
    @FXML private ListView<String> availableProductsList;

    // Left cart pane elements
    @FXML private TableView<Product> cartTable;
    @FXML private TableColumn<Product, String> cartProductNameColumn;
    @FXML private TableColumn<Product, Double> cartPriceColumn;
    @FXML private TableColumn<Product, Integer> cartQuantityColumn;
    // New column for quantity per unit
    @FXML private TableColumn<Product, Integer> cartQuantityPerUnitColumn;
    @FXML private TableColumn<Product, Double> cartTotalColumn;
    @FXML private TableColumn<Product, Void> actionColumn;
    @FXML private Label totalAmountLabel;

    private ObservableList<Product> cart = FXCollections.observableArrayList();
    private Map<String, Product> availableProducts = new HashMap<>();

    @FXML
    public void initialize() {
        // Initialize available products (replace with DB logic if needed)
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

                availableProducts.put(name,new Product(id, name, price, quantity, createdAt, createdBy, active));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Set up available products ListView
        availableProductsList.setItems(FXCollections.observableArrayList(availableProducts.keySet()));
        availableProductsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                searchField.setText(newVal);
            }
        });

        // Filter available products as you type in the search field
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            ObservableList<String> filtered = FXCollections.observableArrayList();
            for (String productName : availableProducts.keySet()) {
                if (productName.toLowerCase().contains(newText.toLowerCase())) {
                    filtered.add(productName);
                }
            }
            availableProductsList.setItems(filtered);
        });

        // Set up cart table columns
        cartProductNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        cartPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        cartQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        // Use a custom cell for Quantity editing
        cartQuantityColumn.setCellFactory(col -> new SpinnerEditingCell());

        // Set up the new Quantity per Unit column
        cartQuantityPerUnitColumn.setCellValueFactory(new PropertyValueFactory<>("quantityperunit"));
        cartQuantityPerUnitColumn.setCellFactory(col -> new SpinnerEditingCell());

        cartTotalColumn.setCellValueFactory(cellData ->
                new SimpleDoubleProperty(cellData.getValue().getPrice() * cellData.getValue().getQuantity()).asObject()
        );

        // Set up action column with a Remove button for each row
        actionColumn.setCellFactory(col -> new TableCell<Product, Void>() {
            private final Button removeButton = new Button("Remove");
            {
                removeButton.setOnAction(e -> {
                    Product product = getTableView().getItems().get(getIndex());
                    cart.remove(product);
                    updateTotalAmount();
                });
                removeButton.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white;");
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeButton);
            }
        });

        cartTable.setItems(cart);
        updateTotalAmount();
    }

    @FXML
    private void handleAddProduct(ActionEvent event) {
        String productName = searchField.getText().trim();
        if (productName.isEmpty()) {
            showAlert("Error", "Please enter a product name!", Alert.AlertType.ERROR);
            return;
        }

        // Get quantity from quantityField (default 1 if empty)
        int quantityToAdd = 1;
        String quantityText = quantityField.getText().trim();
        if (!quantityText.isEmpty()) {
            try {
                quantityToAdd = Integer.parseInt(quantityText);
            } catch (NumberFormatException e) {
                showAlert("Error", "Invalid quantity!", Alert.AlertType.ERROR);
                return;
            }
            if (quantityToAdd <= 0) {
                showAlert("Error", "Quantity must be greater than 0!", Alert.AlertType.ERROR);
                return;
            }
        }

        if (availableProducts.containsKey(productName)) {
            Product selectedProduct = availableProducts.get(productName);
            // Check if the product is already in the cart
            Product cartProduct = findProductInCart(productName);
            if (cartProduct != null) {
                int newQuantity = cartProduct.getQuantity() + quantityToAdd;
                cartProduct.setQuantity(newQuantity);
                // Also update quantity per unit
                cartProduct.setQuantityperunit(newQuantity);
            } else {
                // Create a new product; assign the same quantity to both fields
                Product newProduct = new Product(selectedProduct.getId(), selectedProduct.getName(),
                        selectedProduct.getPrice(), quantityToAdd, LocalDateTime.now(), "User", true);
                newProduct.setQuantityperunit(quantityToAdd);
                cart.add(newProduct);
            }
            cartTable.refresh();
            updateTotalAmount();
        } else {
            showAlert("Not Found", "Product not available!", Alert.AlertType.WARNING);
        }
        // Clear input fields after adding
        searchField.clear();
        quantityField.clear();
    }

    private Product findProductInCart(String productName) {
        return cart.stream().filter(p -> p.getName().equalsIgnoreCase(productName)).findFirst().orElse(null);
    }

    private void updateTotalAmount() {
        double total = cart.stream().mapToDouble(p -> p.getPrice() * p.getQuantity()).sum();
        totalAmountLabel.setText(String.format("Total: $%.2f", total));
    }

    @FXML
    private void handleClearCart(ActionEvent event) {
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

    /**
     * Custom TableCell that uses a Spinner for editing a product's quantity.
     * Updates both the "quantity" and "quantityperunit" fields to keep them in sync.
     */
    private class SpinnerEditingCell extends TableCell<Product, Integer> {
        private final Spinner<Integer> spinner;

        public SpinnerEditingCell() {
            spinner = new Spinner<>(1, 1000, 1);
            spinner.setEditable(true);
            // Update both fields when the spinner's value changes
            spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
                if(getTableRow() != null && getTableRow().getItem() != null) {
                    Product product = getTableRow().getItem();
                    product.setQuantity(newValue);
                    product.setQuantityperunit(newValue);
                    updateTotalAmount();
                    cartTable.refresh();
                }
            });
            // Allow committing by typing directly into the Spinner's editor
            spinner.getEditor().setOnAction(e -> {
                try {
                    int newValue = Integer.parseInt(spinner.getEditor().getText());
                    spinner.getValueFactory().setValue(newValue);
                    if(getTableRow() != null && getTableRow().getItem() != null) {
                        Product product = getTableRow().getItem();
                        product.setQuantity(newValue);
                        product.setQuantityperunit(newValue);
                        updateTotalAmount();
                        cartTable.refresh();
                    }
                } catch (NumberFormatException ex) {
                    // Optionally, notify the user of invalid input
                }
            });
        }

        @Override
        protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            if(empty) {
                setGraphic(null);
            } else {
                spinner.getValueFactory().setValue(item != null ? item : 1);
                setGraphic(spinner);
            }
        }
    }
}
