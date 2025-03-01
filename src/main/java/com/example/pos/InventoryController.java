package com.example.pos;

import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Bill;
import models.Expense;
import models.Product;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import models.AmountReceiveRecord;
public class InventoryController {

    public Button homebutton;
    public CheckBox saleman;
    public CheckBox vendor;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField searchField;
    @FXML private CheckBox cashOutFilterCheckBox;
    @FXML private CheckBox orderby;
    @FXML private TableView<Bill> billsTable;
    @FXML private TableColumn<Bill, Long> billIdColumn;
    @FXML private TableColumn<Bill, Double> cashInColumn;
    @FXML private TableColumn<Bill, Double> cashOutColumn;
    @FXML private TableColumn<Bill, String> createdColumn;
    @FXML private TableColumn<Bill, String> nameColumn;
    @FXML private TableColumn<Bill, String> noteColumn;
    @FXML private TableColumn<Bill, Double> totalColumn;
    @FXML private TableColumn<Bill, Double> discountColumn;
    // Summary Labels (these should be placed in the right pane of your FXML)
    @FXML private Label amountReceivedLabel;
    @FXML private Label amountPendingLabel;
    @FXML private Label discountSummaryLabel;
    @FXML private Label totalSummaryLabel;
    @FXML private ListView<String> availableProductsList;
    private ObservableList<Bill> billsList = FXCollections.observableArrayList();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // Firebird DB connection info.
    private static final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
    private static final String USER = "sysdba";
    private static final String PASSWORD = "123456";
    //@FXML private ListView<String> availableProductsList;
    private Map<String, Product> availableProducts = new HashMap<>();

    @FXML
    public void initialize() {
//        final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
//        final String USER = "sysdba";
//        final String PASSWORD = "123456";
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
        // Set up table columns.
        billIdColumn.setCellValueFactory(new PropertyValueFactory<>("billid"));
        cashInColumn.setCellValueFactory(new PropertyValueFactory<>("cashIn"));
        cashOutColumn.setCellValueFactory(new PropertyValueFactory<>("cashOut"));
        // Use a formatted property "showdate" from Bill. Alternatively, use a lambda to format the "created" timestamp.
        createdColumn.setCellValueFactory(new PropertyValueFactory<>("showdate"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        discountColumn.setCellValueFactory(new PropertyValueFactory<>("discount"));

        // Load bills without filters initially.
        loadBills(null, null, null, false);

        // Set row factory to listen for double-click events.
        billsTable.setRowFactory(tv -> {
            TableRow<Bill> row = new TableRow<>();
            row.setOnMouseClicked((MouseEvent event) -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && (!row.isEmpty())) {
                    Bill clickedBill = row.getItem();
                    handleBillPaymentUpdate(clickedBill);
                }
            });
            return row;
        });

        // Update the summary boxes after initial load.
        updateSummary();
    }

    @FXML
    private void handleFilter(ActionEvent event) {
        applyFilters();
    }

    private void loadBills(LocalDate startDate, LocalDate endDate, String searchText, boolean filterCashOut) {
        billsList.clear();
        StringBuilder sql = new StringBuilder("SELECT billid, cash_in, cash_out, created, is_active, name, note, total, discount,areaid FROM bills WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (startDate != null) {
            sql.append("AND created >= ? ");
            params.add(Timestamp.valueOf(startDate.atStartOfDay()));
        }
        if (endDate != null) {
            sql.append("AND created <= ? ");
            params.add(Timestamp.valueOf(endDate.atTime(LocalTime.MAX)));
        }
        if (searchText != null && !searchText.isEmpty()) {
            sql.append("AND LOWER(name) LIKE ? ");
            params.add("%" + searchText.toLowerCase() + "%");
        }
        if (filterCashOut) {
            sql.append("AND cash_out > 0 ");
        }
        if(saleman.isSelected())
        {

            sql.append("AND areaid is not null ");


        }
        if(vendor.isSelected())
        {


            sql.append("AND vid is not null ");

        }
        if (orderby.isSelected()) {
            sql.append("ORDER BY created ASC ");
        }


        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long billid = rs.getLong("billid");
                    double cashIn = rs.getDouble("cash_in");
                    double cashOut = rs.getDouble("cash_out");
                    Timestamp ts = rs.getTimestamp("created");
                    LocalDateTime created = ts.toLocalDateTime();
                    boolean isActive = rs.getBoolean("is_active");
                    String name = rs.getString("name");
                    String note = rs.getString("note");
                    double total = rs.getDouble("total");
                    double discount = rs.getDouble("discount");
                    long areaid = rs.getLong("areaid");

                    Bill bill = new Bill(billid, cashIn, cashOut, created, isActive, name, note, total, discount,areaid);
                    billsList.add(bill);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error loading bills: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
        billsTable.setItems(billsList);
        updateSummary();
    }

    private void applyFilters() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        String searchText = searchField.getText().trim();
        boolean filterCashOut = cashOutFilterCheckBox.isSelected();
        boolean orderBySelected = orderby.isSelected();
        loadBills(startDate, endDate, searchText, filterCashOut);
    }

    /**
     * Called when a row is double-clicked to update a bill's payment details.
     * Opens a modal dialog with a checkbox and a numeric input for payment.
     * If the checkbox is selected, the entire pending amount (cash_out) is used.
     */
    private void handleBillPaymentUpdate(Bill bill) {
        // Create a dialog to get payment details and a note.
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Update Payment for Bill: " + bill.getBillid() +
                " Name: " + bill.getName() + " Date: " + bill.getShowdate());
        dialog.setHeaderText("Enter Payment Details");

        // Create button types for OK, Detail, Payment Details, and Cancel
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType detailButtonType = new ButtonType("Detail", ButtonBar.ButtonData.OTHER);
        ButtonType paymentDetailButtonType = new ButtonType("Payment Details", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, detailButtonType, paymentDetailButtonType, ButtonType.CANCEL);

        // Build the input grid.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField paymentField = new TextField();
        paymentField.setPromptText("Payment Amount");
        TextFormatter<String> numericFormatter = new TextFormatter<>(change -> {
            if (change.getControlNewText().matches("\\d*(\\.\\d{0,2})?")) {
                return change;
            }
            return null;
        });
        paymentField.setTextFormatter(numericFormatter);

        CheckBox fullPaymentCheckBox = new CheckBox("All Amount Received");
        fullPaymentCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            paymentField.setDisable(newVal);
            if (newVal) {
                paymentField.clear();
            }
        });

        // New field for payment note
        TextField paymentNoteField = new TextField();
        paymentNoteField.setPromptText("Enter payment note");

        grid.add(new Label("Payment Received:"), 0, 0);
        grid.add(paymentField, 1, 0);
        grid.add(fullPaymentCheckBox, 2, 0);
        grid.add(new Label("Note:"), 0, 1);
        grid.add(paymentNoteField, 1, 1, 2, 1); // Span across columns if desired

        dialog.getDialogPane().setContent(grid);

        // Return a Map with keys "payment" and "note"
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                Map<String, String> result = new HashMap<>();
                result.put("payment", fullPaymentCheckBox.isSelected() ? "ALL" : paymentField.getText());
                result.put("note", paymentNoteField.getText());
                return result;
            } else if (dialogButton == detailButtonType) {
                return Collections.singletonMap("payment", "DETAIL");
            } else if (dialogButton == paymentDetailButtonType) {
                return Collections.singletonMap("payment", "PAYMENT_DETAIL");
            }
            return null;
        });

        Optional<Map<String, String>> result = dialog.showAndWait();
        if (result.isPresent()) {
            Map<String, String> paymentData = result.get();
            String paymentInput = paymentData.get("payment");
            // If "DETAIL" button was pressed, show bill details from CART table.
            if ("DETAIL".equals(paymentInput)) {
                showBillDetails(bill.getBillid());
                return;
            }
            // If "PAYMENT_DETAIL" button was pressed, show details from AMOUNT_RECEIVE table.
            if ("PAYMENT_DETAIL".equals(paymentInput)) {
                showPaymentDetailsFromAmountReceive(bill.getBillid());
                return;
            }
            double paymentAmount;
            if ("ALL".equals(paymentInput)) {
                paymentAmount = bill.getCashOut();
            } else {
                try {
                    paymentAmount = Double.parseDouble(paymentInput);
                } catch (NumberFormatException e) {
                    showAlert("Invalid Input", "Please enter a valid number for payment.", Alert.AlertType.ERROR);
                    return;
                }
            }
            double newCashIn = bill.getCashIn() + paymentAmount;
            double newCashOut = bill.getCashOut() - paymentAmount;
            if (newCashOut < 0) {
                newCashOut = 0;
            }
            updateBillPayment(bill.getBillid(), newCashIn, newCashOut);
            bill.setCashIn(newCashIn);
            bill.setCashOut(newCashOut);
            billsTable.refresh();
            updateSummary();

            // Now, insert a record into the AMOUNT_RECEIVE table with the note.
            String paymentNote = paymentData.get("note");
            final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
            final String USER = "sysdba";
            final String PASSWORD = "123456";
            String insertSql = "INSERT INTO AMOUNT_RECEIVE (BILL_ID, NOTE, CREATED_ON, IS_ACTIVE,AMOUNT) " +
                    "VALUES (?, ?, CURRENT_TIMESTAMP, ?,?)";
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setLong(1, bill.getBillid());
                pstmt.setString(2, paymentNote);
                pstmt.setBoolean(3, true);
                pstmt.setDouble(4, paymentAmount);
                pstmt.executeUpdate();
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Database Error", "Error updating payment details: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    /**
     * Opens a modal that shows payment details from the AMOUNT_RECEIVE table for the given bill id.
     */
    private void showPaymentDetailsFromAmountReceive(long billId) {
        Stage paymentStage = new Stage();
        paymentStage.initModality(Modality.APPLICATION_MODAL);
        paymentStage.setTitle("Payment Details for Bill ID: " + billId);

        TableView<AmountReceiveRecord> table = new TableView<>();
        ObservableList<AmountReceiveRecord> list = FXCollections.observableArrayList();

        TableColumn<AmountReceiveRecord, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(80);

        TableColumn<AmountReceiveRecord, String> noteCol = new TableColumn<>("Note");
        noteCol.setCellValueFactory(new PropertyValueFactory<>("note"));
        noteCol.setPrefWidth(200);
        TableColumn<AmountReceiveRecord, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setPrefWidth(180);

        TableColumn<AmountReceiveRecord, LocalDateTime> createdOnCol = new TableColumn<>("Created On");
        createdOnCol.setCellValueFactory(new PropertyValueFactory<>("createdOn"));
        createdOnCol.setPrefWidth(180);
        createdOnCol.setCellFactory(col -> new TableCell<AmountReceiveRecord, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(formatter));
            }
        });

        TableColumn<AmountReceiveRecord, Boolean> activeCol = new TableColumn<>("Active");
        activeCol.setCellValueFactory(new PropertyValueFactory<>("active"));
        activeCol.setPrefWidth(80);

        table.getColumns().addAll(idCol, noteCol,amountCol, createdOnCol, activeCol);

        // Query AMOUNT_RECEIVE table for records matching this bill id.
        final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
        final String USER = "sysdba";
        final String PASSWORD = "123456";
        String sql = "SELECT ID, BILL_ID,amount, NOTE, CREATED_ON, IS_ACTIVE FROM AMOUNT_RECEIVE WHERE BILL_ID = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, billId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("ID");
                    String note = rs.getString("NOTE");
                    Timestamp ts = rs.getTimestamp("CREATED_ON");
                    LocalDateTime createdOn = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                    boolean active = rs.getBoolean("IS_ACTIVE");
                    double amount = rs.getDouble("amount");

                    list.add(new AmountReceiveRecord(id, billId, note, createdOn, active,amount));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Database Error", "Error loading payment details: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
        table.setItems(list);

        VBox layout = new VBox(10, table);
        layout.setPadding(new Insets(10));
        Scene scene = new Scene(layout, 700, 400);
        paymentStage.setScene(scene);
        paymentStage.showAndWait();
    }

    Label grandTotalLabel = new Label();
    private void showBillDetails(long billId) {
        Dialog<Void> detailsDialog = new Dialog<>();
        detailsDialog.setTitle("Bill Details for Bill ID: " + billId);
        detailsDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // First, determine if this bill has an area id.
        Long areaId = null;
        final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
        final String USER = "sysdba";
        final String PASSWORD = "123456";
        String billQuery = "SELECT areaid FROM bills WHERE billid = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(billQuery)) {
            pstmt.setLong(1, billId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    areaId = rs.getLong("areaid");
                    if (rs.wasNull()) {
                        areaId = null;
                    }
                }
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Error checking bill area: " + e.getMessage(), Alert.AlertType.ERROR);
        }

        // Create a TableView to display cart product details.
        TableView<Product> detailsTable = new TableView<>();

        TableColumn<Product, Integer> productIdCol = new TableColumn<>("Product ID");
        productIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Product, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Product, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));

        TableColumn<Product, Integer> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Product, Integer> quantityPerUnitCol = new TableColumn<>("Quantity per Unit");
        quantityPerUnitCol.setCellValueFactory(new PropertyValueFactory<>("quantityPerUnit"));

        TableColumn<Product, Double> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(cellData -> {
            Product p = cellData.getValue();
            Product baseProduct = availableProducts.values().stream()
                    .filter(prod -> prod.getId() == p.getId())
                    .findFirst()
                    .orElse(p); // fallback to p if not found
            //double packagePrice = p.getPrice();
            // Here we assume unit price is computed from the package price divided by quantityPerUnit.
            double packagePrice = p.getPrice();
            double unitPrice = (p.getQuantityPerUnit() > 0) ? packagePrice / baseProduct.getQuantityPerUnit() : 0;
            double total = p.getQuantity() * packagePrice + p.getQuantityPerUnit() * unitPrice;
            return new SimpleDoubleProperty(total).asObject();
        });

        detailsTable.getColumns().addAll(productIdCol, nameCol, priceCol, quantityCol, quantityPerUnitCol, totalCol);

        // If the bill has an area (area checkout), add an "Edit" column.
        if (areaId != null) {
            TableColumn<Product, Void> editCol = new TableColumn<>("Edit");
            editCol.setCellFactory(col -> new TableCell<Product, Void>() {
                private final Button editBtn = new Button("Edit");
                {
                    editBtn.setOnAction(e -> {
                        Product p = getTableView().getItems().get(getIndex());
                        editCartItem(p, billId);
                        // Refresh the table after editing.
                        detailsTable.refresh();
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : editBtn);
                }
            });
            detailsTable.getColumns().add(editCol);
        }

        ObservableList<Product> detailsList = FXCollections.observableArrayList();

        // Load cart items for this bill.
        String query = "SELECT product_id, name, price, quantity, created_on, created_by, is_active, quantity_per_unit " +
                "FROM CART WHERE bill_id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setLong(1, billId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int productId = rs.getInt("product_id");
                    String name = rs.getString("name");
                    double price = rs.getDouble("price");
                    int quantity = rs.getInt("quantity");
                    String createdBy = rs.getString("created_by");
                    boolean isActive = rs.getBoolean("is_active");
                    int quantityPerUnit = rs.getInt("quantity_per_unit");
                    LocalDateTime createdOn = rs.getTimestamp("created_on").toLocalDateTime();
                    // Note: Original price is not used here.
                    Product product = new Product(productId, name, price, quantity, createdOn, createdBy, isActive, quantityPerUnit, 0);
                    detailsList.add(product);
                }
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Error fetching bill details: " + e.getMessage(), Alert.AlertType.ERROR);
            return;
        }

        detailsTable.setItems(detailsList);

        // Compute grand total across all cart items.
        double grandTotal = detailsList.stream()
                .mapToDouble(p -> {
                    Product baseProduct = availableProducts.values().stream()
                            .filter(prod -> prod.getId() == p.getId())
                            .findFirst()
                            .orElse(p); // fallback to p if not found
                    //double packagePrice = p.getPrice();
                    double packagePrice = p.getPrice();
                    double unitPrice = (p.getQuantityPerUnit() > 0) ? packagePrice / baseProduct.getQuantityPerUnit() : 0;
                    return p.getQuantity() * packagePrice + p.getQuantityPerUnit() * unitPrice;
                })
                .sum();

        grandTotalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        grandTotalLabel.setText(String.format("Grand Total: Rs%.2f", grandTotal));

        VBox content = new VBox(10, detailsTable, grandTotalLabel);
        content.setPadding(new Insets(10));

        detailsDialog.getDialogPane().setContent(content);
        detailsDialog.showAndWait();
    }

    /**
     * Opens a dialog to edit a cart item (quantity and quantity per unit) for a given bill.
     */
    private void editCartItem(Product p, long billId) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Cart Item");
        dialog.setHeaderText("Edit item: " + p.getName());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField quantityField = new TextField(String.valueOf(p.getQuantity()));
        TextField quantityPerUnitField = new TextField(String.valueOf(p.getQuantityPerUnit()));

        grid.add(new Label("Quantity:"), 0, 0);
        grid.add(quantityField, 1, 0);
        grid.add(new Label("Quantity per Unit:"), 0, 1);
        grid.add(quantityPerUnitField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int newQuantity = Integer.parseInt(quantityField.getText());
                int newQuantityPerUnit = Integer.parseInt(quantityPerUnitField.getText());
                // Update the product object.
                p.setQuantity(newQuantity);
                p.setQuantityPerUnit(newQuantityPerUnit);
                // Update the record in the CART table.
                final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
                final String USER = "sysdba";
                final String PASSWORD = "123456";
                String updateCartSql = "UPDATE CART SET quantity = ?, quantity_per_unit = ?, is_active = TRUE WHERE product_id = ? AND bill_id = ?";
                try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                     PreparedStatement pstmt = conn.prepareStatement(updateCartSql)) {
                    pstmt.setInt(1, newQuantity);
                    pstmt.setInt(2, newQuantityPerUnit);
                    pstmt.setInt(3, p.getId());
                    pstmt.setLong(4, billId);
                    pstmt.executeUpdate();
                }
                // After editing the cart item, recalc the grand total and update the bill.
                updateBillAfterCartEdit(billId);
            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "Please enter valid numeric values for quantity.", Alert.AlertType.ERROR);
            } catch (SQLException e) {
                showAlert("Database Error", "Error updating cart item: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    /**
     * Recalculates the grand total from the CART records for the given bill,
     * then updates the corresponding bill record with the new total and cash_out.
     */
    private void updateBillAfterCartEdit(long billId) {
        final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
        final String USER = "sysdba";
        final String PASSWORD = "123456";
        double newTotal = 0;
        String query = "SELECT product_id, price, quantity, quantity_per_unit FROM CART WHERE bill_id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setLong(1, billId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int prodId = rs.getInt("product_id");
                    double packagePrice = rs.getDouble("price");
                    int qty = rs.getInt("quantity");
                    int loose = rs.getInt("quantity_per_unit");
                    // Look up the base product to get its standard quantity per unit.
                    Product baseProduct = availableProducts.values().stream()
                            .filter(prod -> prod.getId() == prodId)
                            .findFirst()
                            .orElse(null);
                    double unitPrice = 0;
                    if (baseProduct != null && baseProduct.getQuantityPerUnit() > 0) {
                        unitPrice = packagePrice / baseProduct.getQuantityPerUnit();
                    }
                    double lineTotal = qty * packagePrice + loose * unitPrice;
                    newTotal += lineTotal;
                }

//                Label grandTotalLabel = new Label(String.format("Grand Total: Rs%.2f", newTotal));
//                grandTotalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                grandTotalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                grandTotalLabel.setText(String.format("Grand Total: Rs%.2f", newTotal));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Retrieve current cash_in from the bill record.
        double currentCashIn = 0;
        String query2 = "SELECT cash_in FROM bills WHERE billid = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query2)) {
            pstmt.setLong(1, billId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    currentCashIn = rs.getDouble("cash_in");
                }
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }

        double newCashOut = newTotal - currentCashIn;
        // Update the bill record with the new total and cash_out.
        String updateBillSql = "UPDATE bills SET total = ?, cash_out = ?, updated = CURRENT_TIMESTAMP WHERE billid = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(updateBillSql)) {
            pstmt.setDouble(1, newTotal);
            pstmt.setDouble(2, newCashOut);
            pstmt.setLong(3, billId);
            pstmt.executeUpdate();

            billsTable.refresh();
            updateSummary();
        }


        catch (SQLException e) {
            e.printStackTrace();
        }

        //billsTable.refresh();
    }

    private void updateBillPayment(long billid, double newCashIn, double newCashOut) {
        String sql = "UPDATE bills SET cash_in = ?, cash_out = ?, updated = CURRENT_TIMESTAMP WHERE billid = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newCashIn);
            pstmt.setDouble(2, newCashOut);
            pstmt.setLong(3, billid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("Database Error", "Error updating bill payment: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateSummary() {
        double sumCashIn = 0, sumCashOut = 0, sumDiscount = 0, sumTotal = 0;
        for (Bill bill : billsList) {
            sumCashIn += bill.getCashIn();
            sumCashOut += bill.getCashOut();
            sumDiscount += bill.getDiscount();
            sumTotal += bill.getTotal();
        }
        amountReceivedLabel.setText(String.format("%.2f", sumCashIn));
        amountPendingLabel.setText(String.format("%.2f", sumCashOut));
        discountSummaryLabel.setText(String.format("%.2f", sumDiscount));
        totalSummaryLabel.setText(String.format("%.2f", sumTotal));
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
}
