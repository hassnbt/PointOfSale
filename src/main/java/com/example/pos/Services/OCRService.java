package com.example.pos.Services;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

import java.io.File;

public class OCRService {

    public String extractText(File imageFile) throws Exception {
        ITesseract instance = new Tesseract();
        // Set the path to tessdata folder (adjust the path as needed)
        instance.setDatapath("tessdata");
        // Optionally, set the language (default is English)
        instance.setLanguage("eng");
        return instance.doOCR(imageFile);
    }
}
