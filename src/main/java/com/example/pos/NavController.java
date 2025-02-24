package com.example.pos;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;

public class NavController {

    @FXML
    private void handleInventoryButton(ActionEvent event) {
        switchScene(event, "hello-view.fxml", "Inventory Management");
    }

    @FXML
    private void handleSalesButton(ActionEvent event) {
        switchScene(event, "inventory.fxml", "Bills");
    }

    @FXML
    private void handleReportsButton(ActionEvent event) {
        switchScene(event, "reports.fxml", "Reports");
    }

    @FXML
    private void handleBackButton(ActionEvent event) {
        switchScene(event, "home.fxml", "Home");
    }

    @FXML
    public void handlesalesButton(ActionEvent event) {
        switchScene(event, "home.fxml", "Sales");
    }
    @FXML
    public void handleEmployeeButton(ActionEvent event) {
        switchScene(event, "employee.fxml", "Employee Management");
    }
@FXML
    public void handleVendorsButton(ActionEvent event) {
        switchScene(event, "vendor.fxml", "Vendor Management");

    }
@FXML
    public void handleFactoryVendorButton(ActionEvent event) {
    switchScene(event, "factory-vendor.fxml", "Factory Vendor Management");

}
    /**
     * Utility method to switch scenes with a fade transition.
     * This version always maximizes the stage.
     *
     * @param event    the ActionEvent that triggered the scene change
     * @param fxmlFile the FXML file for the new scene
     * @param title    the title for the new scene's Stage
     */
    private void switchScene(ActionEvent event, String fxmlFile, String title) {
        try {
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
            // Always maximize the stage
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
