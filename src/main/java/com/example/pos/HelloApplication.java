package com.example.pos;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("home.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 2500); // Increased window size

        stage.setTitle("Product Management");
        stage.setScene(scene);

        // Get screen dimensions
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        // Set stage position to center
        stage.setX((screenBounds.getWidth() - 800) / 2);  // Center horizontally
        stage.setY((screenBounds.getHeight() - 500) / 2); // Center vertically
        stage.setMaximized(true);
        stage.show();

    }

    public static void main(String[] args) {
        launch();
    }
}