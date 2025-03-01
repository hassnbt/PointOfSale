package com.example.pos;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Employee;
import models.EmployeeSalary;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class EmployeeController {

    @FXML private TableView<Employee> employeeTable;
    @FXML private TableColumn<Employee, Long> idColumn;
    @FXML private TableColumn<Employee, String> nameColumn;
    @FXML private TableColumn<Employee, LocalDateTime> joiningDateColumn;
    @FXML private TableColumn<Employee, Boolean> activeColumn;
    @FXML private TableColumn<Employee, String> phoneColumn;

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private CheckBox activeCheckBox;
    @FXML private Button addEmployeeButton;

    private ObservableList<Employee> employeeList = FXCollections.observableArrayList();

    // Firebird DB connection info
    private static final String URL = "jdbc:firebirdsql://localhost:3050/C:/firebird/data/DOSACOLA.FDB";
    private static final String USER = "sysdba";
    private static final String PASSWORD = "123456";

    // Formatter for joining date display.
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML
    public void initialize() {
        setupTableColumns();
        addSalaryButtonColumn(); // Add the Salary button column
        loadEmployees();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("eid"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        joiningDateColumn.setCellValueFactory(new PropertyValueFactory<>("joiningDate"));
        joiningDateColumn.setCellFactory(column -> new TableCell<Employee, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.format(dateFormatter));
            }
        });
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        employeeTable.setItems(employeeList);
    }

    /**
     * Adds a new column with a "View Salary" button for each employee.
     */
    private void addSalaryButtonColumn() {
        TableColumn<Employee, Void> salaryCol = new TableColumn<>("Salary");
        salaryCol.setCellFactory(col -> new TableCell<Employee, Void>() {
            private final Button btn = new Button("View Salary");
            {
                btn.setOnAction(e -> {
                    Employee emp = getTableView().getItems().get(getIndex());
                    openSalaryDialog(emp);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        employeeTable.getColumns().add(salaryCol);
    }

    private void loadEmployees() {
        employeeList.clear();
        String sql = "SELECT * FROM employee";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                long eid = rs.getLong("eid");
                String name = rs.getString("name");
                Timestamp ts = rs.getTimestamp("joining_date");
                LocalDateTime joiningDate = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();
                boolean isActive = rs.getBoolean("isactive");
                String phone = rs.getString("phone_number");
                Employee emp = new Employee(eid, name, joiningDate, isActive, phone);
                employeeList.add(emp);
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Error loading employees: " + e.getMessage());
            e.printStackTrace();
        }
        employeeTable.setItems(employeeList);
    }

    @FXML
    private void handleAddEmployee(ActionEvent event) {
        try {
            String name = nameField.getText();
            String phone = phoneField.getText();
            boolean isActive = activeCheckBox.isSelected();

            String sql = "INSERT INTO employee (name, joining_date, isactive, phone_number) VALUES (?, CURRENT_TIMESTAMP, ?, ?)";
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, name);
                pstmt.setBoolean(2, isActive);
                pstmt.setString(3, phone);
                pstmt.executeUpdate();
            }
            loadEmployees();
            clearFields();
        } catch (SQLException e) {
            showAlert("Database Error", "Error adding employee: " + e.getMessage());
        }
    }
    @FXML
    private void handleAddSalary(Employee emp) {
        // Create a TextInputDialog for salary amount input
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Salary");
        dialog.setHeaderText("Add Salary for " + emp.getName());
        dialog.setContentText("Enter salary amount:");

        // Show the dialog and wait for a user response.
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                double amount = Double.parseDouble(result.get().trim());
                // Insert new salary record into employee_salary table.
                String sql = "INSERT INTO employee_salary (emp_id, amount, created_on, isactive) VALUES (?, ?, CURRENT_TIMESTAMP, TRUE)";
                try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, (int) emp.getEid()); // assuming Employee id fits in an int
                    pstmt.setDouble(2, amount);
                    pstmt.executeUpdate();
                }
                showAlert("Success", "Salary added successfully.");
            } catch (NumberFormatException e) {
                showAlert("Input Error", "Invalid salary amount entered.");
            } catch (SQLException e) {
                showAlert("Database Error", "Error adding salary: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void clearFields() {
        nameField.clear();
        phoneField.clear();
        activeCheckBox.setSelected(true);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Opens a modal dialog that displays the salary records for the given employee.
     * Also includes an "Add Salary" button at the top.
     */

    private void openSalaryDialog(Employee emp) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Salary Records for " + emp.getName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        // "Add Salary" button at the top
        Button addSalaryBtn = new Button("Add Salary");
        addSalaryBtn.setOnAction(e -> handleAddSalary(emp));
        layout.getChildren().add(addSalaryBtn);

        // Date filter controls: a single DatePicker to select a month.
        HBox filterBox = new HBox(10);
        filterBox.setPadding(new Insets(5));
        DatePicker monthPicker = new DatePicker();
        monthPicker.setPromptText("Select Month");
        Button filterButton = new Button("Filter");
        filterBox.getChildren().addAll(new Label("Filter by Month:"), monthPicker, filterButton);
        layout.getChildren().add(filterBox);

        // TableView to display salary records
        TableView<EmployeeSalary> salaryTable = new TableView<>();
        TableColumn<EmployeeSalary, Long> sidCol = new TableColumn<>("SID");
        sidCol.setCellValueFactory(new PropertyValueFactory<>("sid"));

        TableColumn<EmployeeSalary, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));

        TableColumn<EmployeeSalary, LocalDateTime> salaryDateCol = new TableColumn<>("Created On");
        salaryDateCol.setCellValueFactory(new PropertyValueFactory<>("createdOn"));
        // Custom cell factory to format the LocalDateTime in a readable format.
        salaryDateCol.setCellFactory(column -> new TableCell<EmployeeSalary, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.format(formatter));
            }
        });

        TableColumn<EmployeeSalary, Boolean> salaryActiveCol = new TableColumn<>("Active");
        salaryActiveCol.setCellValueFactory(new PropertyValueFactory<>("active"));

        salaryTable.getColumns().addAll(sidCol, amountCol, salaryDateCol, salaryActiveCol);

        ObservableList<EmployeeSalary> salaryList = FXCollections.observableArrayList();
        // Initially load all salary records for the employee
        loadEmployeeSalary(emp.getEid(), salaryList, null, null);
        salaryTable.setItems(salaryList);
        layout.getChildren().add(salaryTable);

        // Total label at the bottom.
        Label totalLabel = new Label("Total: 0.00");
        layout.getChildren().add(totalLabel);

        // Update total when the salary list changes.
        salaryList.addListener((javafx.collections.ListChangeListener<EmployeeSalary>) change -> {
            double sum = salaryList.stream().mapToDouble(EmployeeSalary::getAmount).sum();
            totalLabel.setText(String.format("Total: %.2f", sum));
        });

        // Filter button action: apply filter for the selected month.
        filterButton.setOnAction(e -> {
            if (monthPicker.getValue() != null) {
                // Use the selected date as a reference for the month
                java.time.LocalDate selectedDate = monthPicker.getValue();
                LocalDateTime start = selectedDate.withDayOfMonth(1).atStartOfDay();
                LocalDateTime end = selectedDate.withDayOfMonth(selectedDate.lengthOfMonth()).atTime(23, 59, 59);
                loadEmployeeSalary(emp.getEid(), salaryList, start, end);
            } else {
                loadEmployeeSalary(emp.getEid(), salaryList, null, null);
            }
        });

        dialog.getDialogPane().setContent(layout);
        dialog.showAndWait();
    }

    /**
     * Loads salary records for the given employee ID into salaryList.
     * If start and end are provided, filters records within that month.
     */
    private void loadEmployeeSalary(long empId, ObservableList<EmployeeSalary> salaryList, LocalDateTime start, LocalDateTime end) {
        salaryList.clear();
        String sql = "SELECT sid, emp_id, amount, created_on, isactive FROM employee_salary WHERE emp_id = ?";
        if (start != null && end != null) {
            sql += " AND created_on BETWEEN ? AND ?";
        }
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, empId);
            if (start != null && end != null) {
                pstmt.setTimestamp(2, Timestamp.valueOf(start));
                pstmt.setTimestamp(3, Timestamp.valueOf(end));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long sid = rs.getLong("sid");
                    int employeeId = rs.getInt("emp_id");
                    double amount = rs.getDouble("amount");
                    Timestamp ts = rs.getTimestamp("created_on");
                    LocalDateTime createdOn = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();
                    boolean active = rs.getBoolean("isactive");
                    salaryList.add(new EmployeeSalary(sid, employeeId, amount, createdOn, active));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles adding salary for an employee.
     */
//    private void handleAddSalary(Employee emp) {
//        TextInputDialog inputDialog = new TextInputDialog();
//        inputDialog.setTitle("Add Salary");
//        inputDialog.setHeaderText("Add Salary for " + emp.getName());
//        inputDialog.setContentText("Enter salary amount:");
//
//        Optional<String> result = inputDialog.showAndWait();
//        if (result.isPresent()) {
//            try {
//                double amount = Double.parseDouble(result.get().trim());
//                String sql = "INSERT INTO employee_salary (emp_id, amount, created_on, isactive) VALUES (?, ?, CURRENT_TIMESTAMP, TRUE)";
//                try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
//                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
//                    pstmt.setLong(1, emp.getEid());
//                    pstmt.setDouble(2, amount);
//                    pstmt.executeUpdate();
//                }
//                showAlert("Success", "Salary added successfully.", Alert.AlertType.INFORMATION);
//            } catch (NumberFormatException e) {
//                showAlert("Input Error", "Invalid salary amount entered.", Alert.AlertType.ERROR);
//            } catch (SQLException e) {
//                e.printStackTrace();
//                showAlert("Database Error", "Error adding salary: " + e.getMessage(), Alert.AlertType.ERROR);
//            }
//        }
//    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
