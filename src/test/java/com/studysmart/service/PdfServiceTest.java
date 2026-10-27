package com.studysmart.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfServiceTest {
    private final PdfService pdfService = new PdfService();

    @TempDir
    Path tempDir;

    @Test
    void noFileIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> pdfService.extractText(null));
        assertEquals("Please upload a PDF file.", e.getMessage());
    }

    @Test
    void nonPdfIsRejected() throws Exception {
        File txt = Files.writeString(tempDir.resolve("notes.txt"), "hello").toFile();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> pdfService.extractText(txt));
        assertTrue(e.getMessage().startsWith("Invalid file type"));
    }

    @Test
    void emptyPdfIsRejected() throws Exception {
        File pdf = tempDir.resolve("empty.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(pdf);
        }
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> pdfService.extractText(pdf));
        assertEquals("The file contains no readable study material.", e.getMessage());
    }

    @Test
    void textIsExtracted() throws Exception {
        File pdf = tempDir.resolve("notes.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("The mitochondria is the site of respiration.");
                stream.endText();
            }
            doc.save(pdf);
        }
        assertTrue(pdfService.extractText(pdf).contains("mitochondria"));
    }
}
