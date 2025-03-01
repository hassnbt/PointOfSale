package com.example.pos;

import com.example.pos.Services.PrinterService;
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
import models.Employee;
import models.Vendor;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
public class HomeController {

    // Assume each package contains 10 individual units.
    private static final int DEFAULT_UNIT_COUNT = 10;
    private PrinterService printservice = new PrinterService();
    // Scene switching method
//    private void switchScene(ActionEvent event, String fxmlFile, String title) throws IOException {
//        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
//        Parent root = loader.load();
//
//        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
//        Scene scene = new Scene(root, 800, 2500);
//
//        // Apply Fade Transition
//        root.setOpacity(0);
//        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
//        fadeIn.setFromValue(0);
//        fadeIn.setToValue(1);
//        fadeIn.play();
//
//        stage.setScene(scene);
//        stage.setTitle(title);
//        stage.setMaximized(true);
//        stage.show();
//    }
//
//    @FXML
//    private void handleBackButton(ActionEvent event) throws IOException {
//        switchScene(event, "hello-view.fxml", "Dosa Cola POS System");
//    }
//
//    @FXML
//    private void handleInventoryButton(ActionEvent event) throws IOException {
//        switchScene(event, "hello-view.fxml", "Inventory Management");
//    }
//
//    @FXML
//    private void handleSalesButton(ActionEvent event) throws IOException {
//        switchScene(event, "inventory.fxml", "Sales");
//    }
//
//    @FXML
//    private void handleReportsButton(ActionEvent event) throws IOException {
//        switchScene(event, "reports.fxml", "Reports");
//    }

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
        // In your initialize() method:
        quantityField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                // Trigger add-to-cart functionality when Enter is pressed.
                handleAddProduct(new ActionEvent());
                event.consume();
            }});
        manualPriceField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                // Trigger add-to-cart functionality when Enter is pressed.
                handleAddProduct(new ActionEvent());
                event.consume();
            }});

        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                // Trigger add-to-cart functionality when Enter is pressed.
                handleAddProduct(new ActionEvent());
                event.consume();
            } else if (event.getCode() == KeyCode.DOWN) {
                // Move selection down in availableProductsList.
                availableProductsList.getSelectionModel().selectNext();
                availableProductsList.scrollTo(availableProductsList.getSelectionModel().getSelectedIndex());
                event.consume();
            } else if (event.getCode() == KeyCode.UP) {
                // Move selection up in availableProductsList.
                availableProductsList.getSelectionModel().selectPrevious();
                availableProductsList.scrollTo(availableProductsList.getSelectionModel().getSelectedIndex());
                event.consume();
            } else if (event.getCode() == KeyCode.SPACE) {
                // Increase the quantity in quantityField by 10.
                int qty = 0;
                try {
                    qty = Integer.parseInt(quantityField.getText());
                } catch (NumberFormatException ex) {
                    // If the field is empty or invalid, assume 0.
                }
                quantityField.setText(String.valueOf(qty + 10));
                event.consume();
            }
        });

// Global shortcuts (Ctrl+C for checkout, Ctrl+V for clear cart)
        Platform.runLater(() -> {
            Scene scene = searchField.getScene();
            if (scene != null) {
                scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.isControlDown()) {
                        if (event.getCode() == KeyCode.C) {
                            // Ctrl+C triggers checkout.
                            handleCheckout(new ActionEvent());
                            event.consume();
                        } else if (event.getCode() == KeyCode.V) {
                            // Ctrl+V clears the cart.
                            cart.clear();
                            cartTable.refresh();
                            updateTotalAmount();
                            event.consume();
                        }
                    }
                });
            }
        });

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
                    product.setQuantity(0);
                    product.setQuantityPerUnit(0);
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
                searchField.setText("");

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

        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Checkout");
        dialog.setHeaderText("Enter Checkout Details");

        // Create button types for OK, Detail, Payment Details, and Cancel
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType detailButtonType = new ButtonType("Detail", ButtonBar.ButtonData.OTHER);
        ButtonType paymentDetailButtonType = new ButtonType("Payment Details", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, detailButtonType, paymentDetailButtonType, ButtonType.CANCEL);

        // --- Mode Selection: using RadioButtons in a ToggleGroup ---
        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton normalRB = new RadioButton("Normal Checkout");
        normalRB.setToggleGroup(modeGroup);
        normalRB.setSelected(true);
        RadioButton areaRB = new RadioButton("Area Checkout (Employee)");
        areaRB.setToggleGroup(modeGroup);
        RadioButton vendorRB = new RadioButton("Vendor Checkout");
        vendorRB.setToggleGroup(modeGroup);

        HBox modeBox = new HBox(10, normalRB, areaRB, vendorRB);

        // --- Normal Checkout Grid ---
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
        fullAmountCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            amountReceivedField.setDisable(newVal);
            if (newVal) amountReceivedField.clear();
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

        // --- Area Checkout Grid: load employees from DB ---
        GridPane areaGrid = new GridPane();
        areaGrid.setHgap(10);
        areaGrid.setVgap(10);
        areaGrid.setPadding(new Insets(20, 150, 10, 10));
        ComboBox<Employee> employeeComboBox = new ComboBox<>();
        ObservableList<Employee> emp1 = getEmployeesFromDB();
        employeeComboBox.setItems(emp1);
        employeeComboBox.setPromptText("Select Employee");
        areaGrid.add(new Label("Select Employee:"), 0, 0);
        areaGrid.add(employeeComboBox, 1, 0);
        // New: Add Area selection ComboBox for Area Checkout
        ComboBox<Area> areaComboBox = new ComboBox<>();
        ObservableList<Area> areaList = getAreasFromDB();
        areaComboBox.setItems(areaList);
        areaComboBox.setPromptText("Select Area");
        areaGrid.add(new Label("Select Area:"), 0, 1);
        areaGrid.add(areaComboBox, 1, 1);

        // --- Vendor Checkout Grid: load vendors from DB ---
        GridPane vendorGrid = new GridPane();
        vendorGrid.setHgap(10);
        vendorGrid.setVgap(10);
        vendorGrid.setPadding(new Insets(20, 150, 10, 10));
        ComboBox<Vendor> vendorComboBox = new ComboBox<>();
        ObservableList<Vendor> vend = getVendorsFromDB();
        vendorComboBox.setItems(vend);
        vendorComboBox.setPromptText("Select Vendor");
        vendorComboBox.setPrefWidth(200); // increased readability
        vendorGrid.add(new Label("Select Vendor:"), 0, 0);
        vendorGrid.add(vendorComboBox, 1, 0);
        // Create separate controls for vendor mode.
        TextField vendorAmountReceivedField = new TextField();
        vendorAmountReceivedField.setPromptText("Amount Received");
        TextFormatter<String> vendorNumericFormatter = new TextFormatter<>(change -> {
            if (change.getControlNewText().matches("\\d*(\\.\\d{0,2})?")) {
                return change;
            }
            return null;
        });
        vendorAmountReceivedField.setTextFormatter(vendorNumericFormatter);
        CheckBox vendorFullPaymentCheckBox = new CheckBox("All Amount Received");
        vendorFullPaymentCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            vendorAmountReceivedField.setDisable(newVal);
            if (newVal) {
                vendorAmountReceivedField.clear();
            }
        });
        vendorGrid.add(new Label("Amount Received:"), 0, 1);
        vendorGrid.add(vendorAmountReceivedField, 1, 1);
        vendorGrid.add(vendorFullPaymentCheckBox, 2, 1);

        // --- Container for mode selection and dynamic content ---
        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(10));
        contentBox.getChildren().add(modeBox);
        // Initially add normalGrid.
        contentBox.getChildren().add(normalGrid);

        // Listen for changes in mode.
        modeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            contentBox.getChildren().removeAll(normalGrid, areaGrid, vendorGrid);
            RadioButton selected = (RadioButton) newToggle;
            if (selected == normalRB) {
                contentBox.getChildren().add(normalGrid);
            } else if (selected == areaRB) {
                contentBox.getChildren().add(areaGrid);
            } else if (selected == vendorRB) {
                contentBox.getChildren().add(vendorGrid);
            }
        });

        dialog.getDialogPane().setContent(contentBox);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                Map<String, String> result = new HashMap<>();
                RadioButton selected = (RadioButton) modeGroup.getSelectedToggle();
                if (selected == normalRB) {
                    result.put("mode", "normal");
                    result.put("buyerName", buyerNameField.getText());
                    result.put("notes", notesField.getText());
                    result.put("discount", discountField.getText());
                    result.put("amountReceived", fullAmountCheckBox.isSelected() ? "ALL" : amountReceivedField.getText());
                    result.put("vendorId", "");
                    result.put("employeeId", "");
                    result.put("areaId", "");
                } else if (selected == areaRB) {
                    result.put("mode", "area");
                    Employee emp = employeeComboBox.getValue();
                    if (emp == null) return null;
                    result.put("buyerName", emp.getName());
                    result.put("employeeId", String.valueOf(emp.getEid()));
                    Area area = areaComboBox.getValue();
                    if (area == null) return null;
                    result.put("areaId", String.valueOf(area.getAreaid()));
                    result.put("amountReceived", "0");
                    result.put("discount", "0");
                    result.put("notes", "");
                    result.put("vendorId", "");
                } else if (selected == vendorRB) {
                    result.put("mode", "vendor");
                    Vendor ven = vendorComboBox.getValue();
                    if (ven == null) return null;
                    result.put("buyerName", ven.getName());
                    result.put("vendorId", String.valueOf(ven.getVid()));
                    result.put("amountReceived", vendorFullPaymentCheckBox.isSelected() ? "ALL" : vendorAmountReceivedField.getText());
                    result.put("discount", "0");
                    result.put("notes", "");
                    result.put("employeeId", "");
                    result.put("areaId", "");
                }
                return result;
            }
            return null;
        });

        Optional<Map<String, String>> result = dialog.showAndWait();
        if (result.isPresent()) {
            Map<String, String> checkoutData = result.get();
            // Handle "DETAIL" mode if needed (not shown here)
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
                String vendorIdStr = checkoutData.get("vendorId");
                String employeeIdStr = checkoutData.get("employeeId");
                String areaId = checkoutData.get("areaId");


                final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
                final String USER = "sysdba";
                final String PASSWORD = "123456";

                // Insert the bill record.
                int billId = -1;
                String billSql = "INSERT INTO bills (cash_in, cash_out, discount, created, is_active, name, note, total, vid, eid, areaid) " +
                        "VALUES (?, ?, ?, CURRENT_TIMESTAMP, TRUE, ?, ?, ?, ?, ?, ?)";
                try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                     PreparedStatement billPstmt = conn.prepareStatement(billSql, Statement.RETURN_GENERATED_KEYS)) {
                    billPstmt.setDouble(1, amountReceived);
                    billPstmt.setDouble(2, cashOut);
                    billPstmt.setDouble(3, discount);
                    billPstmt.setString(4, buyerName);
                    billPstmt.setString(5, notes);
                    billPstmt.setDouble(6, total);
                    if (vendorIdStr != null && !vendorIdStr.isEmpty()) {
                        billPstmt.setInt(7, Integer.parseInt(vendorIdStr));
                    } else {
                        billPstmt.setNull(7, Types.INTEGER);
                    }
                    if (employeeIdStr != null && !employeeIdStr.isEmpty()) {
                        billPstmt.setInt(8, Integer.parseInt(employeeIdStr));
                    } else {
                        billPstmt.setNull(8, Types.INTEGER);
                    }
                    // For area checkout, if needed. For now, we assume areaId is not used in normal or vendor mode.
                    if(areaId!=null&&!areaId.isEmpty())
                    {
                        billPstmt.setInt(9, Integer.parseInt(areaId));


                    }
                    else
                    {
                        billPstmt.setNull(9, Types.INTEGER);



                    }
                  //  billPstmt.setNull(9, Types.INTEGER);
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
                if (amountReceived != 0) {
                    String insertSql = "INSERT INTO AMOUNT_RECEIVE (BILL_ID, NOTE, CREATED_ON, IS_ACTIVE, AMOUNT) " +
                            "VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?)";
                    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                         PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                        pstmt.setLong(1, billId);
                        pstmt.setString(2, "Bill ka Waqt");
                        pstmt.setBoolean(3, true);
                        pstmt.setDouble(4, amountReceived);
                        pstmt.executeUpdate();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        showAlert("Database Error", "Error updating payment details: " + ex.getMessage(), Alert.AlertType.ERROR);
                    }
                }
                // Insert each cart item into the CART table.
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
                        cartPstmt.setBoolean(8, true);
                        cartPstmt.setInt(9, p.getQuantityPerUnit());
                        cartPstmt.executeUpdate();
                    }
                } catch (SQLException e) {
                    showAlert("Database Error", "Error inserting cart items: " + e.getMessage(), Alert.AlertType.ERROR);
                }

                showAlert("Checkout Successful", "Total Amount: " + totalAmountLabel.getText(), Alert.AlertType.INFORMATION);
                printservice.printBill(cart, total, discount, amountReceived, cashOut, buyerName, notes);
                cart.clear();

                updateTotalAmount();

            } catch (NumberFormatException e) {
                showAlert("Error", "Invalid numeric input!", Alert.AlertType.ERROR);
            } catch (SQLException e) {
                showAlert("Database Error", "Error inserting bill: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private ObservableList<Employee> getEmployeesFromDB(){
        ObservableList<Employee> list = FXCollections.observableArrayList();
        final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
        final String USER = "sysdba";
        final String PASSWORD = "123456";
        String sql = "SELECT eid, name, joining_date, isactive, phone_number FROM employee";
        try(Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){
            while(rs.next()){
                long eid = rs.getLong("eid");
                String name = rs.getString("name");
                Timestamp ts = rs.getTimestamp("joining_date");
                LocalDateTime joiningDate = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();
                boolean active = rs.getBoolean("isactive");
                String phone = rs.getString("phone_number");
                list.add(new Employee(eid, name));
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return list;
    }

    private ObservableList<Vendor> getVendorsFromDB(){
        ObservableList<Vendor> list = FXCollections.observableArrayList();
        final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
        final String USER = "sysdba";
        final String PASSWORD = "123456";
        String sql = "SELECT vid, name, phone_no, address, created_on, is_active FROM vendor";
        try(Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){
            while(rs.next()){
                long vid = rs.getLong("vid");
                String name = rs.getString("name");
                String phoneNo = rs.getString("phone_no");
                String address = rs.getString("address");
                Timestamp ts = rs.getTimestamp("created_on");
                LocalDateTime createdOn = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();
                boolean active = rs.getBoolean("is_active");
                list.add(new Vendor(vid, name));
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return list;
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
            // Listen for changes via spinner's value property.
            spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
                if (getTableRow() != null && getTableRow().getItem() != null) {
                    Product product = getTableRow().getItem();
                    product.setQuantity(newValue);
                    updateTotalAmount();
                    cartTable.refresh();
                }
            });
            // When Enter is pressed, commit the edit.
            spinner.getEditor().setOnKeyReleased(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    try {
                        int newValue = Integer.parseInt(spinner.getEditor().getText());
                        spinner.getValueFactory().setValue(newValue);
                        if (getTableRow() != null && getTableRow().getItem() != null) {
                            Product product = getTableRow().getItem();
                            product.setQuantity(newValue);
                            commitEdit(newValue);
                            updateTotalAmount();
                            cartTable.refresh();
                        }
                    } catch (NumberFormatException ex) {
                        // Optionally notify the user of invalid input.
                    }
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
            spinner.getEditor().setOnKeyReleased(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    try {
                        int newValue = Integer.parseInt(spinner.getEditor().getText());
                        spinner.getValueFactory().setValue(newValue);
                        if (getTableRow() != null && getTableRow().getItem() != null) {
                            Product product = getTableRow().getItem();
                            product.setQuantityPerUnit(newValue);
                            commitEdit(newValue);
                            updateTotalAmount();
                            cartTable.refresh();
                        }
                    } catch (NumberFormatException ex) {
                        // Optionally notify the user of invalid input.
                    }
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
