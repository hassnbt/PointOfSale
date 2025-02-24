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
import models.Employee;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.time.format.DateTimeFormatter;
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
        addStatusActionColumn(); // Add the column with the Toggle Active button.
        loadEmployees();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("eid"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        // Set a custom cell factory for joiningDateColumn to format the date.
        joiningDateColumn.setCellValueFactory(new PropertyValueFactory<>("joiningDate"));
        joiningDateColumn.setCellFactory(column -> {
            return new TableCell<Employee, LocalDateTime>() {
                @Override
                protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.format(dateFormatter));
                    }
                }
            };
        });

        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
    }

    /**
     * Adds a new column with a "Toggle Active" button for each employee.
     */
    private void addStatusActionColumn() {
        TableColumn<Employee, Void> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> new TableCell<Employee, Void>() {
            private final Button toggleButton = new Button("Toggle Active");

            {
                toggleButton.setOnAction(e -> {
                    Employee emp = getTableView().getItems().get(getIndex());
                    boolean newStatus = !emp.isActive();
                    updateEmployeeStatus(emp.getEid(), newStatus);
                    emp.setActive(newStatus);
                    getTableView().refresh();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : toggleButton);
            }
        });
        employeeTable.getColumns().add(actionCol);
    }

    /**
     * Updates the employee's active status in the database.
     */
    private void updateEmployeeStatus(long eid, boolean newStatus) {
        String sql = "UPDATE employee SET isactive = ? WHERE eid = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, newStatus);
            pstmt.setLong(2, eid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("Database Error", "Error updating employee status: " + e.getMessage());
        }
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
        }
        employeeTable.setItems(employeeList);
    }

    @FXML
    private void handleAddEmployee(ActionEvent event) {
        try {
            String name = nameField.getText();
            String phone = phoneField.getText();
            boolean isActive = activeCheckBox.isSelected();

            // Insert the new employee. Joining date defaults to CURRENT_TIMESTAMP.
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

    // Optional: If you need to switch scenes from this page.
    private void switchScene(ActionEvent event, String fxmlFile, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
        Parent root = loader.load();
        Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
        stage.setScene(scene);
        stage.setTitle(title);
        stage.show();
    }
}
