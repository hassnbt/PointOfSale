package com.example.pos;

import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Area;
import models.Product;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HomeController {

    // Assume each package contains 10 individual units.
    private static final int DEFAULT_UNIT_COUNT = 10;

    // Scene switching method
    private void switchScene(ActionEvent event, String fxmlFile, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root, 800, 2500);

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
        switchScene(event, "hello-view.fxml", "Inventory Management");
    }

    @FXML
    private void handleSalesButton(ActionEvent event) throws IOException {
        switchScene(event, "inventory.fxml", "Sales");
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
    // New column for quantity per unit (loose units)
    @FXML private TableColumn<Product, Integer> cartQuantityPerUnitColumn;
    @FXML private TableColumn<Product, Double> cartTotalColumn;
    @FXML private TableColumn<Product, Void> actionColumn;
    @FXML private Label totalAmountLabel;

    private ObservableList<Product> cart = FXCollections.observableArrayList();
    private Map<String, Product> availableProducts = new HashMap<>();
    @FXML
    private CheckBox manualPriceCheckBox;
    @FXML
    private TextField manualPriceField;

    @FXML
    private void toggleManualPriceField() {
        boolean selected = manualPriceCheckBox.isSelected();
        manualPriceField.setVisible(selected);
        manualPriceField.setManaged(selected);
        manualPriceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) { // Allows only digits (0-9)
                manualPriceField.setText(oldValue);
            }
        });// Ensures layout is adjusted
    }
    @FXML
    public void initialize() {
        manualPriceField.setVisible(false);

        // Load available products from the DB using correct column names
        final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
        final String USER = "sysdba";
        final String PASSWORD = "123456";
        // Updated query to fetch all columns consistently
        String query = "SELECT id, name, price, quantity, original_price, quantity_perunit, created_on,is_active FROM PRODUCTS";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                int quantity = rs.getInt("quantity");
                double originalPrice = rs.getDouble("original_price");
                int quantityPerUnit = rs.getInt("quantity_perunit");
                LocalDateTime createdAt = rs.getTimestamp("created_on").toLocalDateTime();
                String createdBy = "";
                boolean active = rs.getBoolean("is_active");

                // Use full constructor with all values
                availableProducts.put(name, new Product(id, name, price, quantity, createdAt, createdBy, active, quantityPerUnit, originalPrice));
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

        // Set up cart table columns using consistent property names from the Product model.
        cartProductNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        cartPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        cartQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        // Custom cell for editing main quantity (full packages)
        cartQuantityColumn.setCellFactory(col -> new SpinnerEditingCellQuantity());

        // Use updated property name "quantityPerUnit" (capital U) in your model.
        cartQuantityPerUnitColumn.setCellValueFactory(new PropertyValueFactory<>("quantityPerUnit"));
        // Custom cell for editing loose units (quantity per unit)
        cartQuantityPerUnitColumn.setCellFactory(col -> new SpinnerEditingCellQuantityPerUnit());

        cartTotalColumn.setCellValueFactory(cellData -> {
            Product p = cellData.getValue();
            // Find the base product from availableProducts by matching the id.
            Product baseProduct = availableProducts.values().stream()
                    .filter(prod -> prod.getId() == p.getId())
                    .findFirst()
                    .orElse(p); // fallback to p if not found
            double packagePrice = p.getPrice();
            double unitPrice = packagePrice / baseProduct.getQuantityPerUnit();
            double total = p.getQuantity() * packagePrice + p.getQuantityPerUnit() * unitPrice;
            return new SimpleDoubleProperty(total).asObject();
        });

        // Remove button for each cart item using a custom cell factory
        actionColumn.setCellFactory(col -> new TableCell<Product, Void>() {
            private final Button removeButton = new Button("Remove");
            {
                removeButton.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white;");
                removeButton.setOnAction(e -> {
                    // Get the current product from the table row.
                    Product product = getTableRow().getItem();
                    if (product != null) {
                        // Optionally, show a confirmation dialog.
                        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                                "Are you sure you want to remove \"" + product.getName() + "\"?",
                                ButtonType.YES, ButtonType.NO);
                        Optional<ButtonType> result = confirmation.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.YES) {
                            cart.remove(product);
                            updateTotalAmount();
                            cartTable.refresh();
                            System.out.println("Removed product: " + product.getName());
                        }
                    }
                });
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

        // Get quantity from quantityField (default is 1 if empty)
        int quantityToAdd = 0;

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
                // Increase only the main quantity (keeping quantity per unit unchanged)
                cartProduct.setQuantity(cartProduct.getQuantity() + quantityToAdd);
            } else {
                // Create a new product: main quantity from input and quantity per unit set to 0.
                Product newProduct = new Product(selectedProduct.getId(), selectedProduct.getName(),
                        selectedProduct.getPrice(), quantityToAdd, LocalDateTime.now(), "User", true,
                        selectedProduct.getQuantityPerUnit(), selectedProduct.getOriginalPrice());
                // If not provided, you can set quantity per unit to 0.
                if(manualPriceCheckBox.isSelected())
                {


                    newProduct.setPrice(Double.parseDouble(manualPriceField.getText().trim()));

                }
                newProduct.setQuantityPerUnit(0);
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
        double total = 0;
        for (Product p : cart) {
//            total += p.getQuantity() * p.getPrice()
//                    + p.getQuantityPerUnit() * (p.getPrice() / p.getQuantityPerUnit());

            Product baseProduct = availableProducts.values().stream()
                    .filter(prod -> prod.getId() == p.getId())
                    .findFirst()
                    .orElse(p); // fallback to p if not found

            double packagePrice = p.getPrice();
            double unitPrice = packagePrice / baseProduct.getQuantityPerUnit();
            total += p.getQuantity() * packagePrice + p.getQuantityPerUnit() * unitPrice;
        }
        totalAmountLabel.setText(String.format("Total: Rs%.2f", total));
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

        // Create a dialog to collect checkout details.
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Checkout");
        dialog.setHeaderText("Enter Checkout Details");

        // Use OK and Cancel buttons.
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        // Create a checkbox to select Area Checkout mode.
        CheckBox alternateCheckoutCheckBox = new CheckBox("Area Checkout");

        // NORMAL CHECKOUT GRID (if not alternate)
        GridPane normalGrid = new GridPane();
        normalGrid.setHgap(10);
        normalGrid.setVgap(10);
        normalGrid.setPadding(new Insets(20, 150, 10, 10));

        TextField amountReceivedField = new TextField();
        amountReceivedField.setPromptText("Amount Received");
        TextField buyerNameField = new TextField();
        buyerNameField.setPromptText("Buyer Name");
        TextField notesField = new TextField();
        notesField.setPromptText("Notes");
        TextField discountField = new TextField();
        discountField.setPromptText("Discount");
        CheckBox fullAmountCheckBox = new CheckBox("All Amount Received");

        fullAmountCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            amountReceivedField.setDisable(isSelected);
            if (isSelected) {
                amountReceivedField.clear();
            }
        });

        normalGrid.add(new Label("Amount Received:"), 0, 0);
        normalGrid.add(amountReceivedField, 1, 0);
        normalGrid.add(fullAmountCheckBox, 2, 0);
        normalGrid.add(new Label("Buyer Name:"), 0, 1);
        normalGrid.add(buyerNameField, 1, 1);
        normalGrid.add(new Label("Notes:"), 0, 2);
        normalGrid.add(notesField, 1, 2);
        normalGrid.add(new Label("Discount:"), 0, 3);
        normalGrid.add(discountField, 1, 3);

        // ALTERNATE CHECKOUT GRID: Two ComboBoxes – one for Area (from DB) and one for Buyer (hard-coded).
        GridPane alternateGrid = new GridPane();
        alternateGrid.setHgap(10);
        alternateGrid.setVgap(10);
        alternateGrid.setPadding(new Insets(20, 150, 10, 10));

        // ComboBox for Area – load from DB using a helper method.
        ComboBox<Area> areaComboBox = new ComboBox<>();
        ObservableList<Area> areas = getAreasFromDB();
        areaComboBox.setItems(areas);
        areaComboBox.setPromptText("Select Area");

        // ComboBox for Buyer Name (hard-coded values).
        ComboBox<String> nameComboBox = new ComboBox<>();
        nameComboBox.setItems(FXCollections.observableArrayList("ali", "sadiq"));
        nameComboBox.setPromptText("Select Buyer");

        alternateGrid.add(new Label("Select Area:"), 0, 0);
        alternateGrid.add(areaComboBox, 1, 0);
        alternateGrid.add(new Label("Select Buyer:"), 0, 1);
        alternateGrid.add(nameComboBox, 1, 1);

        // Container that holds the alternate checkout checkbox and one of the grids.
        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(10));
        contentBox.getChildren().add(alternateCheckoutCheckBox);
        // Default mode is normal mode.
        contentBox.getChildren().add(normalGrid);

        // Switch between normal and alternate grids based on the checkbox.
        alternateCheckoutCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            contentBox.getChildren().remove(normalGrid);
            contentBox.getChildren().remove(alternateGrid);
            if (newVal) {
                contentBox.getChildren().add(alternateGrid);
            } else {
                contentBox.getChildren().add(normalGrid);
            }
        });

        dialog.getDialogPane().setContent(contentBox);

        // Convert the dialog result.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                Map<String, String> result = new HashMap<>();
                if (alternateCheckoutCheckBox.isSelected()) {
                    // Alternate (Area) mode: amountReceived forced to zero.
                    result.put("amountReceived", "0");
                    // Get buyer name from the name combo box.
                    String selectedBuyer = nameComboBox.getValue();
                    if (selectedBuyer == null || selectedBuyer.trim().isEmpty()) {
                        return null; // prevent closing if not selected.
                    }
                    result.put("buyerName", selectedBuyer);
                    // Get area id from the area combo box.
                    Area selectedArea = areaComboBox.getValue();
                    if (selectedArea == null) {
                        return null;
                    }
                    result.put("areaid", String.valueOf(selectedArea.getAreaid()));
                    // For alternate mode, discount and notes default to zero/empty.
                    result.put("discount", "0");
                    result.put("notes", "");
                } else {
                    // Normal mode: use entered values.
                    result.put("buyerName", buyerNameField.getText());
                    result.put("notes", notesField.getText());
                    result.put("discount", discountField.getText());
                    if (fullAmountCheckBox.isSelected()) {
                        result.put("amountReceived", "ALL");
                    } else {
                        result.put("amountReceived", amountReceivedField.getText());
                    }
                    result.put("areaid", null);
                }
                return result;
            }
            return null;
        });

        Optional<Map<String, String>> result = dialog.showAndWait();
        if (result.isPresent()) {
            Map<String, String> checkoutData = result.get();
            try {
                String totalText = totalAmountLabel.getText();
                double total = Double.parseDouble(totalText.replaceAll("[^0-9.]", ""));
                double amountReceived;
                String amtReceivedStr = checkoutData.get("amountReceived");
                if ("ALL".equals(amtReceivedStr)) {
                    amountReceived = total;
                } else {
                    amountReceived = Double.parseDouble(amtReceivedStr);
                }
                double discount = 0;
                String discountStr = checkoutData.get("discount");
                if (discountStr != null && !discountStr.trim().isEmpty()) {
                    discount = Double.parseDouble(discountStr);
                }
                double cashOut = total - amountReceived - discount;
                String buyerName = checkoutData.get("buyerName");
                String notes = checkoutData.get("notes");
                // Retrieve area id if provided.
                String areaIdStr = checkoutData.get("areaid");
                Integer areaId = (areaIdStr != null && !areaIdStr.trim().isEmpty()) ? Integer.parseInt(areaIdStr) : null;

                final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
                final String USER = "sysdba";
                final String PASSWORD = "123456";

                // Insert the bill record (include areaid column).
                int billId = -1;
                String billSql = "INSERT INTO bills (cash_in, cash_out, discount, created, is_active, name, note, total, areaid) " +
                        "VALUES (?, ?, ?, CURRENT_TIMESTAMP, TRUE, ?, ?, ?, ?)";
                try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                     PreparedStatement billPstmt = conn.prepareStatement(billSql, Statement.RETURN_GENERATED_KEYS)) {
                    billPstmt.setDouble(1, amountReceived);
                    billPstmt.setDouble(2, cashOut);
                    billPstmt.setDouble(3, discount);
                    billPstmt.setString(4, buyerName);
                    billPstmt.setString(5, notes);
                    billPstmt.setDouble(6, total);
                    if (areaId != null) {
                        billPstmt.setInt(7, areaId);
                    } else {
                        billPstmt.setNull(7, Types.INTEGER);
                    }
                    billPstmt.executeUpdate();

                    ResultSet generatedKeys = billPstmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        billId = generatedKeys.getInt(1);
                    }
                }

                if (billId == -1) {
                    showAlert("Error", "Failed to retrieve Bill ID.", Alert.AlertType.ERROR);
                    return;
                }

                // Insert each cart item into the CART table as usual.
                String cartSql = "INSERT INTO CART (bill_id, product_id, name, price, quantity, original_price, created_on, created_by, is_active, quantity_per_unit) " +
                        "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?)";
                try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                     PreparedStatement cartPstmt = conn.prepareStatement(cartSql)) {
                    for (Product p : cart) {
                        cartPstmt.setInt(1, billId);
                        cartPstmt.setInt(2, p.getId());
                        cartPstmt.setString(3, p.getName());
                        cartPstmt.setDouble(4, p.getPrice());
                        cartPstmt.setInt(5, p.getQuantity());
                        cartPstmt.setDouble(6, p.getOriginalPrice());
                        cartPstmt.setString(7, p.getCreatedBy() != null ? p.getCreatedBy() : "User");
                        cartPstmt.setBoolean(8, checkoutData.get("areaid") == null);
                        cartPstmt.setInt(9, p.getQuantityPerUnit());
                        cartPstmt.executeUpdate();
                    }
                } catch (SQLException e) {
                    showAlert("Database Error", "Error inserting cart items: " + e.getMessage(), Alert.AlertType.ERROR);
                }

                // In alternate (Area Checkout) mode, we do not update the product table.
//                if (checkoutData.get("areaid") == null) {
//                    // Normal mode: update product quantities.
//                    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
//                        String updateSql = "UPDATE products SET quantity = quantity - ? WHERE id = ?";
//                        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
//                            for (Product p : cart) {
//                                pstmt.setInt(1, p.getQuantity());
//                                pstmt.setInt(2, p.getId());
//                                pstmt.executeUpdate();
//                            }
//                        }
//                    } catch (SQLException e) {
//                        showAlert("Database Error", "Error updating product quantities: " + e.getMessage(), Alert.AlertType.ERROR);
//                    }
//                }

                showAlert("Checkout Successful", "Total Amount: " + totalAmountLabel.getText(), Alert.AlertType.INFORMATION);
                cart.clear();
                updateTotalAmount();

            } catch (NumberFormatException e) {
                showAlert("Error", "Invalid numeric input!", Alert.AlertType.ERROR);
            } catch (SQLException e) {
                showAlert("Database Error", "Error inserting bill: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private ObservableList<Area> getAreasFromDB() {
        ObservableList<Area> areaList = FXCollections.observableArrayList();
        final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
        final String USER = "sysdba";
        final String PASSWORD = "123456";
        String query = "SELECT areaid, name FROM areas WHERE isactive = TRUE";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong("areaid");
                String name = rs.getString("name");
                areaList.add(new Area(id, name));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return areaList;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Custom TableCell for editing the main "quantity" (full packages).
     */
    private class SpinnerEditingCellQuantity extends TableCell<Product, Integer> {
        private final Spinner<Integer> spinner;

        public SpinnerEditingCellQuantity() {
            spinner = new Spinner<>(0, 1000, 0);
            spinner.setEditable(true);
            spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
                if (getTableRow() != null && getTableRow().getItem() != null) {
                    Product product = getTableRow().getItem();
                    product.setQuantity(newValue);
                    updateTotalAmount();
                    cartTable.refresh();
                }
            });
            spinner.getEditor().setOnAction(e -> {
                try {
                    int newValue = Integer.parseInt(spinner.getEditor().getText());
                    spinner.getValueFactory().setValue(newValue);
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        Product product = getTableRow().getItem();
                        product.setQuantity(newValue);
                        updateTotalAmount();
                        cartTable.refresh();
                    }
                } catch (NumberFormatException ex) {
                    // Optionally notify the user of invalid input.
                }
            });
        }

        @Override
        protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                spinner.getValueFactory().setValue(item != null ? item : 1);
                setGraphic(spinner);
            }
        }
    }

    /**
     * Custom TableCell for editing the "quantity per unit" (loose units).
     * Allows a value starting from 0.
     */
    private class SpinnerEditingCellQuantityPerUnit extends TableCell<Product, Integer> {
        private final Spinner<Integer> spinner;

        public SpinnerEditingCellQuantityPerUnit() {
            spinner = new Spinner<>(0, 1000, 0); // Minimum 0 allowed.
            spinner.setEditable(true);
            spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
                if (getTableRow() != null && getTableRow().getItem() != null) {
                    Product product = getTableRow().getItem();
                    product.setQuantityPerUnit(newValue);
                    updateTotalAmount();
                    cartTable.refresh();
                }
            });
            spinner.getEditor().setOnAction(e -> {
                try {
                    int newValue = Integer.parseInt(spinner.getEditor().getText());
                    spinner.getValueFactory().setValue(newValue);
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        Product product = getTableRow().getItem();
                        product.setQuantityPerUnit(newValue);
                        updateTotalAmount();
                        cartTable.refresh();
                    }
                } catch (NumberFormatException ex) {
                    // Optionally notify the user of invalid input.
                }
            });
        }

        @Override
        protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                spinner.getValueFactory().setValue(item != null ? item : 0);
                setGraphic(spinner);
            }
        }



    }
}
