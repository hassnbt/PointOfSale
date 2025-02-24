package com.example.pos;

import com.example.pos.Services.ImageService;
import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.geometry.Insets;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class ImageUploadController {

    @FXML private TextField nameField;
    @FXML private ImageView imageView;

    private File selectedFile;
    // Create an instance of the service layer
    private ImageService imageService = new ImageService();

    /**
     * Handles the "Choose Image" button action.
     * Opens a FileChooser to select an image and displays it.
     */
    @FXML
    public void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif")
        );
        Stage stage = new Stage();
        selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            Image image = new Image(selectedFile.toURI().toString());
            imageView.setImage(image);
        }
    }

    /**
     * Handles the "Capture Image" button action.
     * Opens the webcam preview in a modal window and allows the user
     * to capture an image.
     */
    @FXML
    public void handleCaptureImage() {
        // Create a new modal window for camera preview
        Stage camStage = new Stage();
        camStage.initModality(Modality.APPLICATION_MODAL);
        camStage.setTitle("Camera Preview");

        // ImageView to display the live webcam feed
        ImageView camView = new ImageView();
        camView.setFitWidth(500);
        camView.setPreserveRatio(true);

        // Button to capture the current frame
        Button captureButton = new Button("Capture");
        captureButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5px;");

        VBox camLayout = new VBox(10, camView, captureButton);
        camLayout.setPadding(new Insets(10));
        Scene camScene = new Scene(camLayout, 600, 500);
        camStage.setScene(camScene);
        camStage.show();

        // Get default webcam
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            showAlert("Error", "No webcam detected.");
            return;
        }
        webcam.open();

        // Thread to continuously update the camView with the latest frame
        Thread camThread = new Thread(() -> {
            while (camStage.isShowing() && webcam.isOpen()) {
                BufferedImage img = webcam.getImage();
                if (img != null) {
                    Image fxImg = SwingFXUtils.toFXImage(img, null);
                    Platform.runLater(() -> camView.setImage(fxImg));
                }
                try {
                    Thread.sleep(33); // ~30 fps
                } catch (InterruptedException e) {
                    break;
                }
            }
            if (webcam.isOpen()) {
                webcam.close();
            }
        });
        camThread.setDaemon(true);
        camThread.start();

        // When the user clicks "Capture", set the main ImageView and store the file.
        captureButton.setOnAction(ev -> {
            Image capturedImage = camView.getImage();
            if (capturedImage != null) {
                imageView.setImage(capturedImage);
                try {
                    // Save captured image to a temporary file (optional)
                    File tempFile = File.createTempFile("captured_", ".png");
                    BufferedImage bImage = SwingFXUtils.fromFXImage(capturedImage, null);
                    ImageIO.write(bImage, "PNG", tempFile);
                    selectedFile = tempFile;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlert("Error", "Failed to save captured image: " + ex.getMessage());
                }
            }
            camStage.close();
        });
    }

    /**
     * Handles the "Save Image" button action.
     * Saves the selected or captured image to the database using the service.
     */
    @FXML
    public void handleSaveImage() {
        try {
            String imageName = nameField.getText();
            if (imageName == null || imageName.trim().isEmpty()) {
                showAlert("Error", "Please enter an image name.");
                return;
            }
            if (selectedFile == null) {
                showAlert("Error", "Please select an image file or capture one.");
                return;
            }
            imageService.saveImage(imageName, selectedFile);
            showAlert("Success", "Image saved successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Error saving image: " + e.getMessage());
        }
    }

    /**
     * Handles the "Load Image" button action.
     * Prompts the user for an image ID and loads the image from the database.
     */
    @FXML
    public void handleLoadImage() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Load Image");
        dialog.setHeaderText("Enter Image ID to load:");
        dialog.setContentText("Image ID:");
        dialog.showAndWait().ifPresent(idStr -> {
            try {
                long id = Long.parseLong(idStr);
                Image image = imageService.loadImage(id);
                if (image != null) {
                    imageView.setImage(image);
                    showAlert("Success", "Image loaded successfully.");
                } else {
                    showAlert("Not Found", "No image found with ID: " + id);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Error loading image: " + e.getMessage());
            }
        });
    }

    // Utility method to show alerts
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
