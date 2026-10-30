package com.studysmart.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class PdfService {
    // OpenAI accepts up to 32 MB per request; base64 encoding adds about a third
    public static final long MAX_BYTES = 20L * 1024 * 1024;

    private static final byte[] PDF_HEADER = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    // Checks the file is a real PDF and returns its bytes, ready to send to OpenAI
    public byte[] readPdf(File file) {
        if (file == null) {
            throw new IllegalArgumentException("Please upload a PDF file.");
        }
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Invalid file type. Only PDF files are accepted.");
        }
        if (file.length() > MAX_BYTES) {
            throw new IllegalArgumentException("The PDF is too large. Please upload a file under 20 MB.");
        }

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new IllegalArgumentException("The file could not be read as a PDF.");
        }

        if (!startsWithPdfHeader(bytes)) {
            throw new IllegalArgumentException("The file could not be read as a PDF.");
        }
        return bytes;
    }

    private static boolean startsWithPdfHeader(byte[] bytes) {
        if (bytes.length < PDF_HEADER.length) {
            return false;
        }
        for (int i = 0; i < PDF_HEADER.length; i++) {
            if (bytes[i] != PDF_HEADER[i]) {
                return false;
            }
        }
        return true;
    }
}
