package com.example.pos;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Product;
import models.factoryvendor;
import models.vendorbill;
import models.vendorcart;
import models.payementdetails;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FactoryVendorController {

    @FXML private TableView<factoryvendor> vendorTable;
    @FXML private TableColumn<factoryvendor, Long> idColumn;
    @FXML private TableColumn<factoryvendor, String> nameColumn;
    @FXML private TableColumn<factoryvendor, String> phoneColumn;
    @FXML private TableColumn<factoryvendor, LocalDateTime> createdOnColumn;
    @FXML private TableColumn<factoryvendor, Boolean> activeColumn;

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private CheckBox activeCheckBox;
    @FXML private Button addVendorButton;

    private final ObservableList<factoryvendor> vendorList = FXCollections.observableArrayList();

    // Firebird DB connection details
    private static final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
    private static final String USER = "sysdba";
    private static final String PASSWORD = "123456";

    // Formatter to display createdOn columns in a readable format
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        setupTableColumns();
        loadVendors();
        setupRowDoubleClick();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        createdOnColumn.setCellValueFactory(new PropertyValueFactory<>("createdOn"));
        // Format the createdOn date for factoryvendor rows
        createdOnColumn.setCellFactory(column -> new TableCell<factoryvendor, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(formatter));
            }
        });
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));

        vendorTable.setItems(vendorList); // Link table to observable list
    }

    private void loadVendors() {
        vendorList.clear();
        String sql = "SELECT ID, NAME, PHONE_NO, CREATEDON, ISACTIVE FROM FACTORY_VENDOR";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                long id = rs.getLong("ID");
                String name = rs.getString("NAME");
                String phone = rs.getString("PHONE_NO");
                LocalDateTime createdOn = rs.getTimestamp("CREATEDON") != null
                        ? rs.getTimestamp("CREATEDON").toLocalDateTime()
                        : LocalDateTime.now();
                boolean isActive = rs.getBoolean("ISACTIVE");

                factoryvendor vendor = new factoryvendor(id, name, phone, createdOn, isActive);
                vendorList.add(vendor);
            }

            vendorTable.refresh(); // Refresh table view
        } catch (SQLException e) {
            showAlert("Database Error", "Error loading vendors: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddVendor(ActionEvent event) {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        boolean isActive = activeCheckBox.isSelected();

        if (name.isEmpty() || phone.isEmpty()) {
            showAlert("Input Error", "Name and Phone Number are required.");
            return;
        }

        String sql = "INSERT INTO FACTORY_VENDOR (NAME, PHONE_NO, CREATEDON, ISACTIVE) VALUES (?, ?, CURRENT_TIMESTAMP, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.setBoolean(3, isActive);
            pstmt.executeUpdate();

            loadVendors();
            clearFields();
        } catch (SQLException e) {
            showAlert("Database Error", "Error adding vendor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearFields() {
        nameField.clear();
        phoneField.clear();
        activeCheckBox.setSelected(false);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Set up a double-click listener on each vendor row.
     */
    private void setupRowDoubleClick() {
        vendorTable.setRowFactory(tv -> {
            TableRow<factoryvendor> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2
                        && (!row.isEmpty())) {
                    factoryvendor selectedVendor = row.getItem();
                    showVendorBills(selectedVendor);
                }
            });
            return row;
        });
    }

    /**
     * Opens a modal window that shows all bills for the selected factory vendor.
     * Also includes an "Add Bill" button which opens a new modal to add products to a bill.
     */
    private void showVendorBills(factoryvendor vendor) {
        Stage modalStage = new Stage();
        modalStage.initModality(Modality.APPLICATION_MODAL);
        modalStage.setTitle("Bills for " + vendor.getName());

        // Create an HBox to hold all three buttons in one row
        HBox buttonRow = new HBox(10);
        buttonRow.setPadding(new Insets(10));

        // "Add Bill" button with styling
        Button addBillButton = new Button("Add Bill");
        addBillButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-padding: 8 15 8 15; -fx-background-radius: 5px;");
        addBillButton.setOnAction(e -> openAddBillModal(vendor));

        // "Add Amount" button with styling
        Button paybill = new Button("Add Amount");
        paybill.setStyle("-fx-background-color: #0000FF; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-padding: 8 15 8 15; -fx-background-radius: 5px;");
        paybill.setOnAction(e -> Addamount(vendor));

        // Dummy button with styling
        Button dummyButton = new Button("Show Payment Details");
        dummyButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-padding: 8 15 8 15; -fx-background-radius: 5px;");
        dummyButton.setOnAction(e -> showPaymentDetails(vendor));

        // Add all three buttons to the buttonRow HBox
        buttonRow.getChildren().addAll(addBillButton, paybill, dummyButton);

        // Table for vendor bills with some styling
        TableView<vendorbill> billTable = new TableView<>();
        billTable.setStyle("-fx-background-color: white; -fx-font-size: 13px;");
        ObservableList<vendorbill> billList = FXCollections.observableArrayList();

        TableColumn<vendorbill, Long> bidColumn = new TableColumn<>("BID");
        bidColumn.setCellValueFactory(new PropertyValueFactory<>("bid"));

        TableColumn<vendorbill, Long> fvidColumn = new TableColumn<>("FVID");
        fvidColumn.setCellValueFactory(new PropertyValueFactory<>("fvid"));

        TableColumn<vendorbill, Double> discountColumn = new TableColumn<>("Discount");
        discountColumn.setCellValueFactory(new PropertyValueFactory<>("discount"));

        TableColumn<vendorbill, Double> totalColumn = new TableColumn<>("Total");
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("Total"));

        TableColumn<vendorbill, LocalDateTime> createdOnColumn = new TableColumn<>("Created On");
        createdOnColumn.setCellValueFactory(new PropertyValueFactory<>("createdOn"));
        // Format the createdOn column for vendorbill rows
        createdOnColumn.setCellFactory(column -> new TableCell<vendorbill, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(formatter));
            }
        });

        TableColumn<vendorbill, Boolean> activeColumn = new TableColumn<>("Active");
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));

        billTable.getColumns().addAll(bidColumn, fvidColumn, discountColumn, totalColumn, createdOnColumn, activeColumn);

        // Add row factory to detect double-click on a bill row
        billTable.setRowFactory(tv -> {
            TableRow<vendorbill> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && (!row.isEmpty())) {
                    vendorbill selectedBill = row.getItem();
                    openBillDetailsModal(selectedBill);
                }
            });
            return row;
        });

        // Load vendor bills from DB for the selected vendor
        String sql = "SELECT BID, FVID, AMOUNT_RECEIVED, AMOUNT_PENDING, DISCOUNT, AMOUNT_PENDING_TOTAL, CREATEDON, ISACTIVE FROM VENDOR_BILL WHERE FVID = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, vendor.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long bid = rs.getLong("BID");
                    long fvid = rs.getLong("FVID");
                    double amountReceived = rs.getDouble("AMOUNT_RECEIVED");
                    double amountPending = rs.getDouble("AMOUNT_PENDING");
                    double discount = rs.getDouble("DISCOUNT");
                    double amountPendingTotal = rs.getDouble("AMOUNT_PENDING_TOTAL");
                    LocalDateTime createdOn = rs.getTimestamp("CREATEDON") != null
                            ? rs.getTimestamp("CREATEDON").toLocalDateTime()
                            : LocalDateTime.now();
                    boolean isActive = rs.getBoolean("ISACTIVE");

                    vendorbill bill = new vendorbill(bid, fvid, amountReceived, amountPending,
                            discount, amountPendingTotal, createdOn, isActive);
                    billList.add(bill);
                }
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Error loading vendor bills: " + e.getMessage());
            e.printStackTrace();
        }
        billTable.setItems(billList);

        // Calculate total of all bills (sum of the Total field)
        double totalBills = billList.stream().mapToDouble(b -> b.getTotal()).sum();

        // Query FACTOR_PAID to get total paid amount for this vendor
        double totalPaid = 0.0;
        String paidSql = "SELECT COALESCE(SUM(AMOUNT_PAID), 0) AS totalPaid FROM FACTOR_PAID WHERE FVID = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(paidSql)) {
            pstmt.setLong(1, vendor.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalPaid = rs.getDouble("totalPaid");
                }
            }
        } catch (SQLException ex) {
            showAlert("Database Error", "Error calculating total paid: " + ex.getMessage());
            ex.printStackTrace();
        }
        double remaining = totalBills - totalPaid;

        // Create summary labels with styling
        Label totalBillsLabel = new Label("Total Bills: " + totalBills);
        Label totalPaidLabel = new Label("Total Paid: " + totalPaid);
        Label remainingLabel = new Label("Remaining: " + remaining);
        totalBillsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
        totalPaidLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1565C0;");
        remainingLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #C62828;");

        VBox summaryBox = new VBox(5);
        summaryBox.getChildren().addAll(totalBillsLabel, totalPaidLabel, remainingLabel);
        summaryBox.setStyle("-fx-background-color: #E0F2F1; -fx-padding: 10; -fx-border-color: #B2DFDB; " +
                "-fx-border-width: 2px; -fx-border-radius: 5px; -fx-background-radius: 5px;");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #cccccc; -fx-border-width: 1px; -fx-border-radius: 5px;");
        // Place the buttonRow, then the table, then the summary box
        vbox.getChildren().addAll(buttonRow, billTable, summaryBox);

        Scene scene = new Scene(vbox, 800, 500);
        modalStage.setScene(scene);
        modalStage.showAndWait();
    }

    /**
     * Opens a modal displaying the details of the selected bill.
     * Loads vendor_cart rows for the given bill and calculates the bill total.
     */
    private void openBillDetailsModal(vendorbill bill) {
        Stage detailsStage = new Stage();
        detailsStage.initModality(Modality.APPLICATION_MODAL);
        detailsStage.setTitle("Bill Details for Bill ID: " + bill.getBid());

        // Table for vendor_cart items (bill details)
        TableView<vendorcart> detailsTable = new TableView<>();
        detailsTable.setStyle("-fx-background-color: white; -fx-font-size: 13px;");
        ObservableList<vendorcart> detailsList = FXCollections.observableArrayList();

        TableColumn<vendorcart, String> productNameCol = new TableColumn<>("Product");
        productNameCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName()));

        TableColumn<vendorcart, Integer> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<vendorcart, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));

        TableColumn<vendorcart, Double> rowTotalCol = new TableColumn<>("Row Total");
        rowTotalCol.setCellValueFactory(cellData -> {
            vendorcart item = cellData.getValue();
            double rowTotal = item.getPrice() * item.getQuantity();
            return new SimpleDoubleProperty(rowTotal).asObject();
        });

        detailsTable.getColumns().addAll(productNameCol, quantityCol, priceCol, rowTotalCol);

        // Load vendor_cart items for the given bill id
        String cartSql = "SELECT VID, BILL_ID, PRODUCT_ID, \"name\", PRICE, QUANTITY, ORIGINAL_PRICE, CREATED_ON, CREATED_BY, IS_ACTIVE, QUANTITY_PER_UNIT FROM VENDOR_CART WHERE BILL_ID = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(cartSql)) {
            pstmt.setLong(1, bill.getBid());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long vid = rs.getLong("VID");
                    long billId = rs.getLong("BILL_ID");
                    long productId = rs.getLong("PRODUCT_ID");
                    String name = rs.getString("NAME");
                    double price = rs.getDouble("PRICE");
                    int quantity = rs.getInt("QUANTITY");
                    double originalPrice = rs.getDouble("ORIGINAL_PRICE");
                    LocalDateTime createdOn = rs.getTimestamp("CREATED_ON") != null
                            ? rs.getTimestamp("CREATED_ON").toLocalDateTime() : LocalDateTime.now();
                    String createdBy = rs.getString("CREATED_BY");
                    boolean isActive = rs.getBoolean("IS_ACTIVE");
                    int quantityPerUnit = rs.getInt("QUANTITY_PER_UNIT");

                    vendorcart item = new vendorcart(vid, billId, productId, name, price, quantity, originalPrice,
                            createdOn, createdBy, isActive, quantityPerUnit);
                    detailsList.add(item);
                }
            }
        } catch (SQLException ex) {
            showAlert("Database Error", "Error loading bill details: " + ex.getMessage());
            ex.printStackTrace();
        }

        detailsTable.setItems(detailsList);

        // Calculate the total for the bill (sum of price * quantity)
        double totalBill = detailsList.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
        Label totalLabel = new Label("Total: " + totalBill);
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Style the container layout
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #cccccc; " +
                "-fx-border-width: 1px; -fx-border-radius: 5px;");
        layout.getChildren().addAll(detailsTable, totalLabel);

        // Optionally, add a styled close button at the bottom
        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-padding: 8 15 8 15; -fx-background-radius: 5px;");
        closeButton.setOnAction(e -> detailsStage.close());
        layout.getChildren().add(closeButton);

        Scene scene = new Scene(layout, 600, 400);
        detailsStage.setScene(scene);
        detailsStage.showAndWait();
    }

    /**
     * Opens a new modal for adding a bill. In this modal the user can:
     * - Select a product (loaded from the PRODUCTS table)
     * - Enter quantity and price
     * - Add multiple products to the bill (displayed in a TableView)
     * - Submit the bill (inserting the entries into VENDOR_CART)
     */
    private void openAddBillModal(factoryvendor vendor) {
        Stage addBillStage = new Stage();
        addBillStage.initModality(Modality.APPLICATION_MODAL);
        addBillStage.setTitle("Add Bill for " + vendor.getName());

        // --- Create form elements ---
        Label productLabel = new Label("Select Product:");
        ComboBox<Product> productComboBox = new ComboBox<>();
        ObservableList<Product> productList = FXCollections.observableArrayList();
        // Load products from PRODUCTS table
        String productSql = "SELECT r.ID, r.\"NAME\", r.PRICE, r.ORIGINAL_PRICE, r.QUANTITY, r.QUANTITY_PERUNIT, r.CREATED_ON, r.IS_ACTIVE FROM PRODUCTS r";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(productSql)) {
            while (rs.next()) {
                int id = rs.getInt("ID");
                String name = rs.getString("NAME");
                double price = rs.getDouble("PRICE");
                double originalPrice = rs.getDouble("ORIGINAL_PRICE");
                int quantity = rs.getInt("QUANTITY");
                int quantityPerUnit = rs.getInt("QUANTITY_PERUNIT");
                LocalDateTime createdOn = rs.getTimestamp("CREATED_ON") != null
                        ? rs.getTimestamp("CREATED_ON").toLocalDateTime()
                        : LocalDateTime.now();
                boolean isActive = rs.getBoolean("IS_ACTIVE");
                Product product = new Product(id, name, price, quantity, createdOn, "system", isActive, quantityPerUnit, originalPrice);
                productList.add(product);
            }
        } catch (SQLException ex) {
            showAlert("Database Error", "Error loading products: " + ex.getMessage());
            ex.printStackTrace();
        }
        productComboBox.setItems(productList);

        Label quantityLabel = new Label("Quantity:");
        TextField quantityField = new TextField();
        Label priceLabel = new Label("Price:");
        TextField priceField = new TextField();

        Button addProductButton = new Button("Add Product");

        // --- Table to display products added to the bill ---
        TableView<vendorcart> billItemTable = new TableView<>();
        ObservableList<vendorcart> billItemList = FXCollections.observableArrayList();

        TableColumn<vendorcart, String> productNameColumn = new TableColumn<>("Product");
        productNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName()));
        productNameColumn.setPrefWidth(250);

        TableColumn<vendorcart, Integer> billQuantityColumn = new TableColumn<>("Quantity");
        billQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        billQuantityColumn.setPrefWidth(100);

        TableColumn<vendorcart, Double> billPriceColumn = new TableColumn<>("Price");
        billPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        billPriceColumn.setPrefWidth(150);
        Label totalLabel = new Label("Total: 0.00");
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Utility method to update total
        Runnable updateTotal = () -> {
            double total = 0.0;
            for (vendorcart item : billItemList) {
                total += item.getPrice() * item.getQuantity();
            }
            totalLabel.setText("Total: " + total);
        };
        // --- Remove Button Column ---
        TableColumn<vendorcart, Void> removeColumn = new TableColumn<>("Action");
        removeColumn.setPrefWidth(100);
        removeColumn.setCellFactory(col -> new TableCell<vendorcart, Void>() {
            private final Button removeButton = new Button("Remove");
            {
                removeButton.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 5px;");
                removeButton.setOnAction(event -> {
                    vendorcart data = getTableView().getItems().get(getIndex());
                    getTableView().getItems().remove(data);
                    updateTotal.run();
                });
            }
            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeButton);
            }
        });

        billItemTable.getColumns().addAll(productNameColumn, billQuantityColumn, billPriceColumn, removeColumn);
        billItemTable.setItems(billItemList);

        // --- Label to display total ---


        // --- "Add Product" button action ---
        addProductButton.setOnAction(ev -> {
            Product selectedProduct = productComboBox.getSelectionModel().getSelectedItem();
            if (selectedProduct == null) {
                showAlert("Input Error", "Please select a product.");
                return;
            }
            int quantity;
            double price;
            try {
                quantity = Integer.parseInt(quantityField.getText());
                price = Double.parseDouble(priceField.getText());
            } catch (NumberFormatException nfe) {
                showAlert("Input Error", "Please enter valid numeric values for quantity and price.");
                return;
            }
            // Create a new vendorcart object. Bill ID will be updated after bill insertion.
            vendorcart billItem = new vendorcart(
                    vendor.getId(),        // Vendor ID
                    0,                     // Bill ID (placeholder)
                    selectedProduct.getId(),
                    selectedProduct.getName(),
                    price,
                    quantity,
                    selectedProduct.getOriginalPrice(),
                    LocalDateTime.now(),
                    "system",              // createdBy (could be dynamic)
                    true,
                    0
            );
            billItemList.add(billItem);
            // Clear inputs for next entry
            productComboBox.getSelectionModel().clearSelection();
            quantityField.clear();
            priceField.clear();
            updateTotal.run();
        });

        // --- Submit and Cancel buttons ---
        Button submitBillButton = new Button("Submit Bill");
        submitBillButton.setOnAction(ev -> {
            // First, calculate the total amount for the bill.
            double totalAmount = 0.0;
            for (vendorcart item : billItemList) {
                totalAmount += item.getPrice() * item.getQuantity();
            }

            // Insert into VENDOR_BILL table and get the generated bill id.
            String billInsertSql = "INSERT INTO VENDOR_BILL (FVID, AMOUNT_RECEIVED, AMOUNT_PENDING, DISCOUNT, AMOUNT_PENDING_TOTAL, CREATEDON, ISACTIVE) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)";
            long generatedBillId = 0;
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement billPstmt = conn.prepareStatement(billInsertSql, Statement.RETURN_GENERATED_KEYS)) {
                // For a new bill, assume amountReceived and discount are zero.
                billPstmt.setLong(1, vendor.getId());
                billPstmt.setDouble(2, 0.0);           // amount received
                billPstmt.setDouble(3, totalAmount);   // amount pending
                billPstmt.setDouble(4, 0.0);             // discount
                billPstmt.setDouble(5, totalAmount);     // total pending
                billPstmt.setBoolean(6, true);           // isActive
                billPstmt.executeUpdate();

                try (ResultSet rs = billPstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedBillId = rs.getLong(1);
                    }
                }
            } catch (SQLException ex) {
                showAlert("Database Error", "Error inserting vendor bill: " + ex.getMessage());
                ex.printStackTrace();
                return;
            }

            // Now insert each vendor_cart entry with the generated bill id.
            String insertSql = "INSERT INTO VENDOR_CART (VID, BILL_ID, PRODUCT_ID, \"name\", PRICE, QUANTITY, ORIGINAL_PRICE, CREATED_ON, CREATED_BY, IS_ACTIVE, QUANTITY_PER_UNIT) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, true, ?)";
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (vendorcart item : billItemList) {
                    pstmt.setLong(1, item.getVid());
                    pstmt.setLong(2, generatedBillId); // Use the generated bill id
                    pstmt.setLong(3, item.getProductId());
                    pstmt.setString(4, item.getName());
                    pstmt.setDouble(5, item.getPrice());
                    pstmt.setInt(6, item.getQuantity());
                    pstmt.setDouble(7, item.getOriginalPrice());
                    pstmt.setTimestamp(8, Timestamp.valueOf(item.getCreatedOn()));
                    pstmt.setString(9, item.getCreatedBy());
                    pstmt.setInt(10, item.getQuantityPerUnit());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            } catch (SQLException ex) {
                showAlert("Database Error", "Error submitting bill: " + ex.getMessage());
                ex.printStackTrace();
                return;
            }

            // --- Update product table: add bill quantity to existing product quantity ---
            String updateSql = "UPDATE PRODUCTS SET QUANTITY = QUANTITY + ? WHERE ID = ?";
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                for (vendorcart item : billItemList) {
                    updatePstmt.setInt(1, item.getQuantity());
                    updatePstmt.setLong(2, item.getProductId());
                    updatePstmt.addBatch();
                }
                updatePstmt.executeBatch();
            } catch (SQLException ex) {
                showAlert("Database Error", "Error updating product quantities: " + ex.getMessage());
                ex.printStackTrace();
                return;
            }

            showAlert("Success", "Bill submitted successfully and product quantities updated.");
            addBillStage.close();
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(ev -> addBillStage.close());

        // --- Layout the Add Bill modal ---
        HBox form = new HBox(10);
        form.getChildren().addAll(productLabel, productComboBox, quantityLabel, quantityField, priceLabel, priceField, addProductButton);
        HBox buttons = new HBox(10);
        buttons.getChildren().addAll(submitBillButton, cancelButton);
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        // Add the form, table, running total label, then the buttons
        layout.getChildren().addAll(form, billItemTable, totalLabel, buttons);

        Scene scene = new Scene(layout, 900, 400);
        addBillStage.setScene(scene);
        addBillStage.showAndWait();
    }

    private void Addamount(factoryvendor vendor) {
        Stage payStage = new Stage();
        payStage.initModality(Modality.APPLICATION_MODAL);
        payStage.setTitle("Add Amount for " + vendor.getName());

        // Create input fields for Note and Amount
        Label noteLabel = new Label("Note:");
        TextField noteField = new TextField();
        Label amountLabel = new Label("Amount:");
        TextField amountField = new TextField();

        // Create Submit and Cancel buttons with styling
        Button submitButton = new Button("Submit");
        submitButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; "
                + "-fx-font-size: 14px; -fx-padding: 8 15 8 15; -fx-background-radius: 5px;");
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; "
                + "-fx-font-size: 14px; -fx-padding: 8 15 8 15; -fx-background-radius: 5px;");
        cancelButton.setOnAction(e -> payStage.close());

        // Layout for input fields
        HBox form = new HBox(10);
        form.getChildren().addAll(noteLabel, noteField, amountLabel, amountField);
        // Layout for buttons
        HBox buttons = new HBox(10);
        buttons.getChildren().addAll(submitButton, cancelButton);
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.getChildren().addAll(form, buttons);

        Scene scene = new Scene(layout, 500, 150);
        payStage.setScene(scene);
        payStage.show();

        // On submit, validate input and insert into FACTOR_PAID table
        submitButton.setOnAction(e -> {
            double amount;
            try {
                amount = Double.parseDouble(amountField.getText());
            } catch (NumberFormatException ex) {
                showAlert("Input Error", "Please enter a valid numeric amount.");
                return;
            }
            String note = noteField.getText().trim();

            String insertSql = "INSERT INTO FACTOR_PAID (FVID, AMOUNT_PAID, NOTE, ISACTIVE, CREATED_ON) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setLong(1, vendor.getId());  // Insert vendor id into FVID column
                pstmt.setDouble(2, amount);
                pstmt.setString(3, note);
                pstmt.setBoolean(4, true);
                pstmt.executeUpdate();
                showAlert("Success", "Amount added successfully.");
                payStage.close();
            } catch (SQLException ex) {
                showAlert("Database Error", "Error inserting payment: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }


    private void showPaymentDetails(factoryvendor vendor) {
        Stage paymentStage = new Stage();
        paymentStage.initModality(Modality.APPLICATION_MODAL);
        paymentStage.setTitle("Payment Details");

        TableView<payementdetails> table = new TableView<>();
        ObservableList<payementdetails> paymentList = FXCollections.observableArrayList();

        TableColumn<payementdetails, Long> vidColumn = new TableColumn<>("VID");
        vidColumn.setCellValueFactory(new PropertyValueFactory<>("vid"));

        TableColumn<payementdetails, Long> fvidColumn = new TableColumn<>("FVID");
        fvidColumn.setCellValueFactory(new PropertyValueFactory<>("fvid"));

        TableColumn<payementdetails, Double> amountPaidColumn = new TableColumn<>("Amount Paid");
        amountPaidColumn.setCellValueFactory(new PropertyValueFactory<>("amountPaid"));

        TableColumn<payementdetails, String> noteColumn = new TableColumn<>("Note");
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));

        TableColumn<payementdetails, Boolean> activeColumn = new TableColumn<>("Active");
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));

        TableColumn<payementdetails, LocalDateTime> createdOnColumn = new TableColumn<>("Created On");
        createdOnColumn.setCellValueFactory(new PropertyValueFactory<>("createdOn"));
        // Format the createdOn column using the same formatter
        createdOnColumn.setCellFactory(col -> new TableCell<payementdetails, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(formatter));
            }
        });

        table.getColumns().addAll(vidColumn, fvidColumn, amountPaidColumn, noteColumn, activeColumn, createdOnColumn);

        // Load payment details from FACTOR_PAID table
        String sql = "SELECT VID, FVID, AMOUNT_PAID, NOTE, ISACTIVE, CREATED_ON FROM FACTOR_PAID where FVID="+vendor.getId();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                long vid = rs.getLong("VID");
                long fvid = rs.getLong("FVID");
                double amountPaid = rs.getDouble("AMOUNT_PAID");
                String note = rs.getString("NOTE");
                boolean active = rs.getBoolean("ISACTIVE");
                LocalDateTime createdOn = rs.getTimestamp("CREATED_ON") != null
                        ? rs.getTimestamp("CREATED_ON").toLocalDateTime()
                        : LocalDateTime.now();
                payementdetails payment = new payementdetails(vid, fvid, amountPaid, note, active, createdOn);
                paymentList.add(payment);
            }
        } catch (SQLException ex) {
            showAlert("Database Error", "Error loading payment details: " + ex.getMessage());
            ex.printStackTrace();
        }
        table.setItems(paymentList);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().add(table);

        Scene scene = new Scene(vbox, 800, 400);
        paymentStage.setScene(scene);
        paymentStage.showAndWait();
    }

}
