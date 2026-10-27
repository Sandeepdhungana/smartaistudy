package com.studysmart.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

public class PdfService {
    // Keeps the request to OpenAI a reasonable size for very large PDFs
    public static final int MAX_CHARS = 15000;

    public String extractText(File file) {
        if (file == null) {
            throw new IllegalArgumentException("Please upload a PDF file.");
        }
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Invalid file type. Only PDF files are accepted.");
        }

        String text;
        try (PDDocument document = Loader.loadPDF(file)) {
            text = new PDFTextStripper().getText(document).trim();
        } catch (IOException e) {
            throw new IllegalArgumentException("The file could not be read as a PDF.");
        }

        if (text.isEmpty()) {
            throw new IllegalArgumentException("The file contains no readable study material.");
        }
        if (text.length() > MAX_CHARS) {
            text = text.substring(0, MAX_CHARS);
        }
        return text;
    }
}
