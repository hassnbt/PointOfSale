package com.example.pos;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import models.Product;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class HelloController {

    // Buttons (make sure fx:id values match in FXML)
    @FXML public Button addProductButton;
    @FXML public Button deleteProductButton;
    @FXML public Button homeButton;

    // Input fields for adding a product
    @FXML private TextField productNameField, priceField, originalPriceField, quantityField, quantityPerUnitField, searchField;

    // Table and its columns (only the ones to display)
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, String> productColumn;
    @FXML private TableColumn<Product, Double> priceColumn;
    @FXML private TableColumn<Product, Double> originalPriceColumn;
    @FXML private TableColumn<Product, Integer> quantityColumn;
    @FXML private TableColumn<Product, Integer> quantityPerUnitColumn;

    // Actions column for per-row buttons
    @FXML private TableColumn<Product, Void> actionsColumn;

    private ObservableList<Product> productList = FXCollections.observableArrayList();
    private FilteredList<Product> filteredList;
    private Connection conn;

    // Formatter for date strings if needed (not shown in the UI)
    private final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        connectToDatabase();
        setupTableColumns();
        setupActionsColumn();  // Set up custom cell with Update and Delete buttons.
        loadProducts();
        setupSearchFilter();
    }

    private void connectToDatabase() {
        try {
            // Firebird connection URL (adjust path, user, and password as needed)
            String url = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
            String user = "sysdba";
            String password = "123456";
            conn = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            showAlert("Database Error", "Unable to connect to database: " + e.getMessage());
        }
    }

    private void setupTableColumns() {
        // Ensure these property names match your Product model getters
        productColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        originalPriceColumn.setCellValueFactory(new PropertyValueFactory<>("originalPrice"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityPerUnitColumn.setCellValueFactory(new PropertyValueFactory<>("quantityPerUnit"));
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<Product, Void>() {
            private final Button updateBtn = new Button("Update");
            private final Button deleteBtn = new Button("Delete");
            private final HBox pane = new HBox(5, updateBtn, deleteBtn);

            {
                // Update button opens a dialog to change quantity, price, and original price.
                updateBtn.setOnAction(e -> {
                    Product product = getTableView().getItems().get(getIndex());
                    Dialog<ButtonType> dialog = new Dialog<>();
                    dialog.setTitle("Update Product");
                    dialog.setHeaderText("Update details for " + product.getName());

                    GridPane grid = new GridPane();
                    grid.setHgap(10);
                    grid.setVgap(10);
                    grid.setPadding(new Insets(20, 150, 10, 10));

                    TextField quantityField = new TextField(String.valueOf(product.getQuantity()));
                    TextField priceField = new TextField(String.valueOf(product.getPrice()));
                    TextField originalPriceField = new TextField(String.valueOf(product.getOriginalPrice()));

                    grid.add(new Label("Quantity:"), 0, 0);
                    grid.add(quantityField, 1, 0);
                    grid.add(new Label("Price:"), 0, 1);
                    grid.add(priceField, 1, 1);
                    grid.add(new Label("Original Price:"), 0, 2);
                    grid.add(originalPriceField, 1, 2);

                    dialog.getDialogPane().setContent(grid);
                    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

                    Optional<ButtonType> result = dialog.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        try {
                            int newQuantity = Integer.parseInt(quantityField.getText());
                            double newPrice = Double.parseDouble(priceField.getText());
                            double newOriginalPrice = Double.parseDouble(originalPriceField.getText());
                            updateProductDetails(product, newQuantity, newPrice, newOriginalPrice);
                        } catch (NumberFormatException ex) {
                            showAlert("Invalid Input", "Please enter valid numeric values.");
                        }
                    }
                });

                // Delete button confirms deletion before removing product.
                deleteBtn.setOnAction(e -> {
                    Product product = getTableView().getItems().get(getIndex());
                    Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                            "Are you sure you want to delete product \"" + product.getName() + "\"?",
                            ButtonType.YES, ButtonType.NO);
                    Optional<ButtonType> response = confirmation.showAndWait();
                    if (response.isPresent() && response.get() == ButtonType.YES) {
                        deleteProduct(product);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadProducts() {
        productList.clear();
        String sql = "SELECT * FROM PRODUCTS";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                double originalPrice = rs.getDouble("original_price");
                int quantity = rs.getInt("quantity");
                int quantityPerUnit = rs.getInt("quantity_perunit");
                // For simplicity, we set createdOn to now, createdBy to an empty string, isActive to true.
                Product prod = new Product(id, name, price, quantity, LocalDateTime.now(), "", true, quantityPerUnit, originalPrice);
                productList.add(prod);
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Error loading products: " + e.getMessage());
        }
        filteredList = new FilteredList<>(productList, p -> true);
        productTable.setItems(filteredList);
    }

    private void setupSearchFilter() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            filteredList.setPredicate(product -> {
                if (newValue == null || newValue.isEmpty()) return true;
                return product.getName().toLowerCase().contains(newValue.toLowerCase());
            });
        });
    }

    @FXML
    private void handleAddProduct(ActionEvent event) {
        try {
            String name = productNameField.getText();
            double price = Double.parseDouble(priceField.getText());
            double originalPrice = Double.parseDouble(originalPriceField.getText());
            int quantity = Integer.parseInt(quantityField.getText());
            int quantityPerUnit = Integer.parseInt(quantityPerUnitField.getText());

            // Insert using CURRENT_TIMESTAMP for created_on and default TRUE for is_active.
            String sql = "INSERT INTO products (name, price, original_price, quantity, quantity_perunit, created_on, is_active) " +
                    "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, TRUE)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, name);
                pstmt.setDouble(2, price);
                pstmt.setDouble(3, originalPrice);
                pstmt.setInt(4, quantity);
                pstmt.setInt(5, quantityPerUnit);
                pstmt.executeUpdate();
            }
            loadProducts();
            clearFields();
        } catch (Exception e) {
            showAlert("Invalid Input", "Please check your input values. Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteProduct(ActionEvent event) {
        // This method is retained for external delete button usage, if needed.
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            showAlert("No Selection", "Please select a product to delete.");
            return;
        }
        deleteProduct(selectedProduct);
    }

    private void updateProductDetails(Product product, int newQuantity, double newPrice, double newOriginalPrice) {
        String sql = "UPDATE products SET quantity = ?, price = ?, original_price = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newQuantity);
            pstmt.setDouble(2, newPrice);
            pstmt.setDouble(3, newOriginalPrice);
            pstmt.setInt(4, product.getId());
            pstmt.executeUpdate();
            loadProducts();
        } catch (SQLException e) {
            showAlert("Database Error", "Error updating product: " + e.getMessage());
        }
    }

    private void deleteProduct(Product product) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, product.getId());
            pstmt.executeUpdate();
            loadProducts();
        } catch (SQLException e) {
            showAlert("Database Error", "Error deleting product: " + e.getMessage());
        }
    }

    private void clearFields() {
        productNameField.clear();
        priceField.clear();
        originalPriceField.clear();
        quantityField.clear();
        quantityPerUnitField.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
