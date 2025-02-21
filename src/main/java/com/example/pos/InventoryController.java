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
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Bill;
import models.Product;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class InventoryController {

    public Button homebutton;
    public CheckBox saleman;
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
        StringBuilder sql = new StringBuilder("SELECT billid, cash_in, cash_out, created, is_active, name, note, total, discount FROM bills WHERE 1=1 ");
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

                    Bill bill = new Bill(billid, cashIn, cashOut, created, isActive, name, note, total, discount);
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
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Update Payment for Bill: " + bill.getBillid() +
                " Name: " + bill.getName() + " Date: " + bill.getShowdate());
        dialog.setHeaderText("Enter Payment Details");

        // Create button types for OK, Detail, and Cancel
        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType detailButtonType = new ButtonType("Detail", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, detailButtonType, ButtonType.CANCEL);

        // Build the input grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

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
        fullPaymentCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            paymentField.setDisable(isSelected);
            if (isSelected) {
                paymentField.clear();
            }
        });

        grid.add(new Label("Payment Received:"), 0, 0);
        grid.add(paymentField, 1, 0);
        grid.add(fullPaymentCheckBox, 2, 0);

        dialog.getDialogPane().setContent(grid);

        // Return "ALL", the payment amount, or "DETAIL" based on the button clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return fullPaymentCheckBox.isSelected() ? "ALL" : paymentField.getText();
            } else if (dialogButton == detailButtonType) {
                return "DETAIL";
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String paymentInput = result.get();
            if ("DETAIL".equals(paymentInput)) {
                // Open a new modal showing all cart items for the same bill id.
                showBillDetails(bill.getBillid());
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
        }
    }

    private void showBillDetails(long billId) {
        Dialog<Void> detailsDialog = new Dialog<>();
        detailsDialog.setTitle("Bill Details for Bill ID: " + billId);
        detailsDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Create a TableView to display product details.
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
            // Look up the corresponding base product to get its quantity per unit from the product table.
            Product baseProduct = availableProducts.values().stream()
                    .filter(prod -> prod.getId() == p.getId())
                    .findFirst()
                    .orElse(p); // fallback to p if not found
            double packagePrice = p.getPrice();
            // Calculate the unit price using the base product's quantity per unit.
            double unitPrice = packagePrice / baseProduct.getQuantityPerUnit();
            // Calculate the total as (full packages * package price) plus (loose units * unit price).
            double total = p.getQuantity() * packagePrice + p.getQuantityPerUnit() * unitPrice;
            return new SimpleDoubleProperty(total).asObject();
        });


        detailsTable.getColumns().addAll(productIdCol, nameCol, priceCol, quantityCol, quantityPerUnitCol, totalCol);

        ObservableList<Product> detailsList = FXCollections.observableArrayList();

        final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
        final String USER = "sysdba";
        final String PASSWORD = "123456";
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

        // Compute grand total across all cart items
        double grandTotal = detailsList.stream()
                .mapToDouble(p -> {
                    // Look up the corresponding base product for the correct quantity per unit
                    Product baseProduct = availableProducts.values().stream()
                            .filter(prod -> prod.getId() == p.getId())
                            .findFirst()
                            .orElse(p); // fallback to p if not found
                    double packagePrice = p.getPrice();
                    double unitPrice = packagePrice / baseProduct.getQuantityPerUnit();
                    // Calculate line total: full packages + loose units cost
                    return p.getQuantity() * packagePrice + p.getQuantityPerUnit() * unitPrice;
                })
                .sum();


        Label grandTotalLabel = new Label(String.format("Grand Total: Rs%.2f", grandTotal));
        VBox content = new VBox(10, detailsTable, grandTotalLabel);
        content.setPadding(new Insets(10));

        detailsDialog.getDialogPane().setContent(content);
        detailsDialog.showAndWait();
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
