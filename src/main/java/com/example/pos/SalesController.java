package com.example.pos;

import javafx.collections.transformation.FilteredList;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import models.Expense;
import models.ProductSalesSummary;
import com.example.pos.Services.ExpenseService;
import com.example.pos.Services.SalesService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SalesController {

    public Label soldTotalSummaryLabel;
    public Label factoryTotalSummaryLabel;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    //@FXML private DatePicker centerDatePicker;
    @FXML private Button searchButton;
    @FXML private TableView<ProductSalesSummary> tableView;
    @FXML private TableColumn<ProductSalesSummary, Long> idColumn;
    @FXML private TableColumn<ProductSalesSummary, String> nameColumn;
    @FXML private TableColumn<ProductSalesSummary, Double> soldQuantityColumn;
    @FXML private TableColumn<ProductSalesSummary, Double> soldTotalColumn;
    @FXML private TableColumn<ProductSalesSummary, Double> soldPerUnitColumn;
    @FXML private TableColumn<ProductSalesSummary, Double> factoryQuantityColumn;
    @FXML private TableColumn<ProductSalesSummary, Double> factoryTotalColumn;
    @FXML private TableColumn<ProductSalesSummary, Double> factorLooseCostColumn;
    @FXML private TableColumn<ProductSalesSummary, Double> quantityPerUnitColumn;

    @FXML private Label amountReceivedLabel;
    @FXML private Label amountPendingLabel;
    @FXML private Label discountLabel;
    @FXML private Label totalLabel;

    @FXML private Label expenseTotalLabel;
    @FXML private Label factorPaidTotalLabel;
    @FXML private Label empSalaryTotalLabel;
    @FXML private Label totalexpence;



//    // Expense controls from bottom region
//    @FXML private DatePicker expenseDatePicker;
//    @FXML private TextField expenseNoteField;
//    @FXML private Button addExpenseButton;
    @FXML private Button showExpenseButton;

    private SalesService salesService = new SalesService();
    private ExpenseService expenseService = new ExpenseService();
    private ObservableList<ProductSalesSummary> data = FXCollections.observableArrayList();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    @FXML
    public void initialize() {
        // Initialize table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        soldQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("soldQuantity"));
        soldTotalColumn.setCellValueFactory(new PropertyValueFactory<>("soldTotal"));
        soldPerUnitColumn.setCellValueFactory(new PropertyValueFactory<>("soldPerUnitQuantity"));
        factoryQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("factoryQuantity"));
        factoryTotalColumn.setCellValueFactory(new PropertyValueFactory<>("factoryTotal"));
        factorLooseCostColumn.setCellValueFactory(new PropertyValueFactory<>("factorLooseCost"));
        quantityPerUnitColumn.setCellValueFactory(new PropertyValueFactory<>("quantityPerUnit"));

        tableView.setItems(data);

        // Set default dates for top date pickers
        LocalDate today = LocalDate.now();
        fromDatePicker.setValue(today.withDayOfMonth(1));
        toDatePicker.setValue(today);
        //centerDatePicker.setValue(today);

        searchButton.setOnAction((e) -> {

                search();

             updateAllSummaries();

        
        });

        // Expense controls: open modal for adding expense
        //addExpenseButton.setOnAction(e -> showAddExpenseModal());
        showExpenseButton.setOnAction(e -> showExpensesModal());
    }

    private void search() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();
        if (fromDate == null || toDate == null) {
            showAlert("Error", "Please select both start and end dates.");
            return;
        }
        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(23, 59, 59);
        try {
            List<ProductSalesSummary> list = salesService.getProductSalesSummary(start, end);
            data.setAll(list);
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Error", "Error loading data: " + ex.getMessage());
        }
    }

    /**
     * Opens a modal dialog that asks for expense note, expense amount, and extra amount.
     * If the user clicks OK, the values are passed to ExpenseService.addExpense().
     */
    private void showAddExpenseModal() {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Add New Expense");
        dialog.setHeaderText("Enter Expense Details");

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField noteField = new TextField();
        noteField.setPromptText("Expense Note");
        TextField amountField = new TextField();
        amountField.setPromptText("Expense Amount");
//        TextField extraAmountField = new TextField();
//        extraAmountField.setPromptText("Extra Amount");

        grid.add(new Label("Expense Note:"), 0, 0);
        grid.add(noteField, 1, 0);
        grid.add(new Label("Expense Amount:"), 0, 1);
        grid.add(amountField, 1, 1);
//        grid.add(new Label("Extra Amount:"), 0, 2);
//        grid.add(extraAmountField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                Map<String, String> result = new HashMap<>();
                result.put("note", noteField.getText());
                result.put("amount", amountField.getText());

                return result;
            }
            return null;
        });

        Optional<Map<String, String>> result = dialog.showAndWait();
        if (result.isPresent()) {
            Map<String, String> expenseData = result.get();
            String note = expenseData.get("note");
            double amount = 0.0;

            try {
                amount = Double.parseDouble(expenseData.get("amount"));

            } catch (NumberFormatException ex) {
                showAlert("Error", "Invalid amount values.");
                return;
            }
            try {
                expenseService.addExpense(note, amount);
                showAlert("Success", "Expense added successfully.");
            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Error", "Error adding expense: " + ex.getMessage());
            }
        }
    }

    private void showExpensesModal() {
        Stage expenseStage = new Stage();
        expenseStage.initModality(Modality.APPLICATION_MODAL);
        expenseStage.setTitle("Expense Details");

        // Table to display expenses
        TableView<Expense> expenseTable = new TableView<>();
        ObservableList<Expense> expenseList = FXCollections.observableArrayList();

        TableColumn<Expense, Long> expIdCol = new TableColumn<>("ID");
        expIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        expIdCol.setPrefWidth(80);

        TableColumn<Expense, String> notesCol = new TableColumn<>("Notes");
        notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));
        notesCol.setPrefWidth(300);

        TableColumn<Expense, LocalDateTime> createdOnCol = new TableColumn<>("Created On");
        createdOnCol.setCellValueFactory(new PropertyValueFactory<>("createdOn"));
        createdOnCol.setPrefWidth(180);
        createdOnCol.setCellFactory(col -> new TableCell<Expense, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(formatter));
            }
        });

        TableColumn<Expense, Boolean> activeCol = new TableColumn<>("Active");
        activeCol.setCellValueFactory(new PropertyValueFactory<>("active"));
        activeCol.setPrefWidth(80);

        TableColumn<Expense, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setPrefWidth(120);

        expenseTable.getColumns().addAll(expIdCol, notesCol, amountCol, createdOnCol, activeCol);

        // Load expenses from the database
        try {
            List<Expense> expenses = expenseService.getAllExpenses();
            expenseList.setAll(expenses);
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error", "Could not load expenses: " + ex.getMessage());
        }

        // Wrap expenseList in a FilteredList for date filtering
        FilteredList<Expense> filteredExpenses = new FilteredList<>(expenseList, e -> true);
        expenseTable.setItems(filteredExpenses);

        // Date filter controls (for modal-level filtering)
        DatePicker fromExpenseDatePicker = new DatePicker();
        fromExpenseDatePicker.setPromptText("From Date");
        DatePicker toExpenseDatePicker = new DatePicker();
        toExpenseDatePicker.setPromptText("To Date");
        Button filterButton = new Button("Filter");
        filterButton.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 14px;");

        HBox filterBox = new HBox(10, new Label("Expense Date:"), fromExpenseDatePicker, toExpenseDatePicker, filterButton);
        filterBox.setPadding(new Insets(10));

        // Filter action: update the predicate based on selected dates
        filterButton.setOnAction(e -> {
            LocalDate fromDate = fromExpenseDatePicker.getValue();
            LocalDate toDate = toExpenseDatePicker.getValue();
            filteredExpenses.setPredicate(expense -> {
                if (fromDate == null && toDate == null) {
                    return true;
                }
                LocalDate expenseDate = expense.getCreatedOn().toLocalDate();
                if (fromDate != null && expenseDate.isBefore(fromDate)) {
                    return false;
                }
                if (toDate != null && expenseDate.isAfter(toDate)) {
                    return false;
                }
                return true;
            });
        });

        // Button to add a new expense within the modal
        Button addNewExpenseButton = new Button("Add New Expense");
        addNewExpenseButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        addNewExpenseButton.setOnAction(e -> {
            showAddExpenseModal();
            try {
                List<Expense> expenses = expenseService.getAllExpenses();
                expenseList.setAll(expenses);
            } catch (SQLException ex) {
                showAlert("Error", "Could not reload expenses: " + ex.getMessage());
            }
        });

        VBox layout = new VBox(10, filterBox, expenseTable, addNewExpenseButton);
        layout.setPadding(new Insets(10));
        Scene scene = new Scene(layout, 700, 400);
        expenseStage.setScene(scene);
        expenseStage.showAndWait();
    }
    private void updateAllSummaries() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();
        if (fromDate == null || toDate == null) {
            showAlert("Error", "Please select both From and To dates for summary.");
            return;
        }
        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(23, 59, 59);

        final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
        final String USER = "sysdba";
        final String PASSWORD = "123456";

        // -------------------
        // 1. Update BILLS Summary & TableView computed sums
        // -------------------
        double amountReceived = 0.0;
        double total=0.0;
        double discount=0.0;
        double amountPending=0.0;
        String billsSql = "SELECT SUM(CASH_IN) AS amountReceived, " +
                "SUM(CASH_OUT) AS amountPending, " +
                "SUM(DISCOUNT) AS discount, " +
                "SUM(TOTAL) AS total " +
                "FROM BILLS WHERE CREATED BETWEEN ? AND ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(billsSql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(start));
            pstmt.setTimestamp(2, Timestamp.valueOf(end));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    amountReceived = rs.getDouble("amountReceived");
                     amountPending = rs.getDouble("amountPending");
                     discount = rs.getDouble("discount");
                     total = rs.getDouble("total");

                    System.out.println("BILLS -> amountReceived: " + amountReceived +
                            ", amountPending: " + amountPending +
                            ", discount: " + discount +
                            ", total: " + total);


                    amountPendingLabel.setText(String.valueOf(amountPending));
                    discountLabel.setText(String.valueOf(discount));
                    totalLabel.setText(String.valueOf(total));

                    // Also calculate factoryTotal and soldTotal from tableView items
                    double totalFactoryTotal = tableView.getItems()
                            .stream()
                            .mapToDouble(ProductSalesSummary::getFactoryTotal)
                            .sum();
                    double totalSoldTotal = tableView.getItems()
                            .stream()
                            .mapToDouble(ProductSalesSummary::getSoldTotal)
                            .sum();
                    factoryTotalSummaryLabel.setText(String.valueOf(totalFactoryTotal));
                    soldTotalSummaryLabel.setText(String.valueOf(totalSoldTotal));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error", "Error updating bill summary: " + ex.getMessage());
        }

        // -------------------
        // 2. Update other amount summaries from EMPLOYEE_SALARY, EXPENSE, FACTOR_PAID
        // -------------------
        double empSalaryTotal = 0.0;
        double expenseTotal = 0.0;
        double factorPaidTotal = 0.0;

        String empSql = "SELECT COALESCE(SUM(AMOUNT), 0) AS total FROM EMPLOYEE_SALARY WHERE CREATED_ON BETWEEN ? AND ?";
        String expenseSql = "SELECT COALESCE(SUM(AMOUNT), 0) AS total FROM EXPENSE WHERE CREATED_ON BETWEEN ? AND ?";
        String factorPaidSql = "SELECT COALESCE(SUM(AMOUNT_PAID), 0) AS total FROM FACTOR_PAID WHERE CREATED_ON BETWEEN ? AND ?";
        String amountrecieve = "SELECT COALESCE(SUM(amount), 0) AS total FROM AMOUNT_RECEIVE WHERE CREATED_ON BETWEEN ? AND ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            // Employee Salary Total
            try (PreparedStatement pstmt = conn.prepareStatement(empSql)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(start));
                pstmt.setTimestamp(2, Timestamp.valueOf(end));
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        empSalaryTotal = rs.getDouble("total");
                    }
                }
            }
            try (PreparedStatement pstmt = conn.prepareStatement(amountrecieve)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(start));
                pstmt.setTimestamp(2, Timestamp.valueOf(end));
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                       // empSalaryTotal = rs.getDouble("total");
                        amountReceivedLabel.setText(String.valueOf(rs.getDouble("total")));
                    }
                }
            }
            // Expense Total
            try (PreparedStatement pstmt = conn.prepareStatement(expenseSql)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(start));
                pstmt.setTimestamp(2, Timestamp.valueOf(end));
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        expenseTotal = rs.getDouble("total");
                    }
                }
            }
            // Factor Paid Total
            try (PreparedStatement pstmt = conn.prepareStatement(factorPaidSql)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(start));
                pstmt.setTimestamp(2, Timestamp.valueOf(end));
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        factorPaidTotal = rs.getDouble("total");
                    }
                }
            }

            System.out.println("EMPLOYEE_SALARY Total: " + empSalaryTotal);
            System.out.println("EXPENSE Total: " + expenseTotal);
            System.out.println("FACTOR_PAID Total: " + factorPaidTotal);

            empSalaryTotalLabel.setText(String.valueOf(empSalaryTotal));
            expenseTotalLabel.setText(String.valueOf(expenseTotal));
            factorPaidTotalLabel.setText(String.valueOf(factorPaidTotal));

          //  totalexpence.setText(String.valueOf(empSalaryTotal+expenseTotal+discount));

        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert("Error", "Error updating amount summaries: " + ex.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
