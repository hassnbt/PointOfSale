package com.example.pos.Services;

import javafx.embed.swing.SwingFXUtils;
import javafx.print.PrinterJob;
import javafx.scene.SnapshotParameters;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import models.Product;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PrinterService {

    public void printBill(List<Product> cart, double total, double discount, double amountReceived, double cashOut, String buyerName, String notes) {
        // Prepare date formatter for header
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy HH:mm");
        String currentDateTime = LocalDateTime.now().format(dtf);

        // Build the bill text with improved design.
        StringBuilder sb = new StringBuilder();
        sb.append("***************************************************************************\n");

        sb.append("                                  Dosa Cola\n");
        sb.append("                                Factory Outlet\n");
        sb.append("                               Tel: 0305-4733362\n");
        sb.append("***************************************************************************\n");

        sb.append("Date: ").append(currentDateTime).append("\n");
        sb.append("Buyer: ").append(buyerName).append("\n");
        sb.append("Notes: ").append(notes).append("\n");
        sb.append("***************************************************************************\n");
        sb.append(String.format("%-20s %5s %10s %10s\n", "Item", "Qty", "Price", "Total"));
        sb.append("***************************************************************************\n");

        for (Product p : cart) {
            double itemTotal = p.getPrice() * p.getQuantity();
            sb.append(String.format("%-20s %5d %10.2f %10.2f\n", p.getName(), p.getQuantity(), p.getPrice(), itemTotal));
        }
        sb.append("***************************************************************************\n");

        sb.append(String.format("Amount Received: %.2f\n", amountReceived));
        sb.append(String.format("Discount:        %.2f\n", discount));
        sb.append(String.format("Cash Out:        %.2f\n", cashOut));
        sb.append(String.format("Total:           %.2f\n", total));
        sb.append("***************************************************************************\n");

        sb.append("                         Thank you for your purchase!\n");
        sb.append("***************************************************************************\n");


        // Create a TextArea to display the bill preview
        TextArea billTextArea = new TextArea(sb.toString());
        billTextArea.setEditable(false);
        // Increase font size and use monospace for alignment
        billTextArea.setStyle("-fx-font-family: monospace; -fx-font-size: 18px; -fx-text-fill: #333;");
        // Optionally, disable wrapping if you prefer horizontal scrolling
        billTextArea.setWrapText(true);

        // Create a Print button
        Button printButton = new Button("Print Bill");
        printButton.setOnAction(e -> {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null && job.showPrintDialog(billTextArea.getScene().getWindow())) {
                boolean printed = job.printPage(billTextArea);
                if (printed) {
                    job.endJob();
                }
            }
        });

        // Create a Save as PNG button using a temporary Text node
        Button saveButton = new Button("Save as PNG");
        saveButton.setOnAction(e -> {
            // Use a Text node (non-scrollable) to capture the entire bill content.
            Text textNode = new Text(sb.toString());
            textNode.setStyle("-fx-font-family: monospace; -fx-font-size: 18px; -fx-text-fill: #333;");
            // Set wrapping width so that the snapshot is taken at desired width.
            textNode.setWrappingWidth(380);
            SnapshotParameters params = new SnapshotParameters();
            WritableImage snapshot = textNode.snapshot(params, null);

            // FileChooser to select where to save the image.
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Bill as PNG");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Files", "*.png"));
            // Set an initial file name
            String initialFileName = buyerName.replaceAll("\\s+", "_") + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".png";
            fileChooser.setInitialFileName(initialFileName);
            File file = fileChooser.showSaveDialog(billTextArea.getScene().getWindow());
            if (file != null) {
                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", file);
                    showAlert("Success", "Bill saved as " + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    showAlert("Error", "Failed to save bill as PNG: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });

        HBox buttonBox = new HBox(10, printButton, saveButton);
        buttonBox.setPadding(new Insets(10));
        VBox layout = new VBox(10, billTextArea, buttonBox);
        layout.setPadding(new Insets(10));

        Stage billStage = new Stage();
        billStage.initModality(Modality.APPLICATION_MODAL);
        billStage.setTitle("Bill Preview");
        billStage.setScene(new Scene(layout, 450, 400));
        billStage.showAndWait();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
