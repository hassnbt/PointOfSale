package com.example.pos.Services;

import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import models.Product;

import java.util.List;

public class PrinterService{
// Assuming Product has: getName(), getQuantity(), getPrice()
public void printBill(List<Product> cart, double total, double discount, double amountReceived, double cashOut, String buyerName, String notes) {
    // Build the bill string (you can adjust the formatting as needed)
    StringBuilder sb = new StringBuilder();
    sb.append("---------- BILL ----------\n");
    sb.append("Buyer: ").append(buyerName).append("\n");
    sb.append("Notes: ").append(notes).append("\n");
    sb.append("--------------------------\n");
    sb.append(String.format("%-20s %5s %10s %10s\n", "Item", "Qty", "Price", "Total"));
    sb.append("--------------------------\n");
    for (Product p : cart) {
        double itemTotal = p.getPrice() * p.getQuantity();
        sb.append(String.format("%-20s %5d %10.2f %10.2f\n", p.getName(), p.getQuantity(), p.getPrice(), itemTotal));
    }
    sb.append("--------------------------\n");
    sb.append(String.format("Amount Received: %.2f\n", amountReceived));
    sb.append(String.format("Discount:        %.2f\n", discount));
    sb.append(String.format("Cash Out:        %.2f\n", cashOut));
    sb.append(String.format("Total:           %.2f\n", total));
    sb.append("--------------------------\n");
    sb.append("Thank you for your purchase!\n");

    // Create a TextArea to display the bill
    TextArea billTextArea = new TextArea(sb.toString());
    billTextArea.setEditable(false);
    billTextArea.setStyle("-fx-font-family: monospace; -fx-font-size: 14px;");

    // Create a Print button to print the bill using PrinterJob
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

    VBox layout = new VBox(10, billTextArea, printButton);
    layout.setPadding(new Insets(10));
    Stage billStage = new Stage();
    billStage.initModality(Modality.APPLICATION_MODAL);
    billStage.setTitle("Bill Preview");
    billStage.setScene(new Scene(layout, 400, 300));
    billStage.showAndWait();

}
}
