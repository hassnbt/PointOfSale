package com.example.pos;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import models.Bill;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InventoryController {

    public Button homebutton;
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

    private ObservableList<Bill> billsList = FXCollections.observableArrayList();

    // Firebird DB connection info.
    private static final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
    private static final String USER = "sysdba";
    private static final String PASSWORD = "123456";

    @FXML
    public void initialize() {
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
        dialog.setTitle("Update Payment for Bill: " + bill.getBillid() + " Name: " + bill.getName() + " Date: " + bill.getShowdate());
        dialog.setHeaderText("Enter Payment Details");

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

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
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return fullPaymentCheckBox.isSelected() ? "ALL" : paymentField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String paymentInput = result.get();
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
}
