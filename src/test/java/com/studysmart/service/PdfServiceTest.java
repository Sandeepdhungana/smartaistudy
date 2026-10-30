package com.studysmart.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfServiceTest {
    private final PdfService pdfService = new PdfService();

    @TempDir
    Path tempDir;

    @Test
    void noFileIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> pdfService.readPdf(null));
        assertEquals("Please upload a PDF file.", e.getMessage());
    }

    @Test
    void nonPdfIsRejected() throws Exception {
        File txt = Files.writeString(tempDir.resolve("notes.txt"), "hello").toFile();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> pdfService.readPdf(txt));
        assertTrue(e.getMessage().startsWith("Invalid file type"));
    }

    @Test
    void renamedNonPdfIsRejected() throws Exception {
        File fake = Files.writeString(tempDir.resolve("notes.pdf"), "just some text").toFile();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> pdfService.readPdf(fake));
        assertEquals("The file could not be read as a PDF.", e.getMessage());
    }

    @Test
    void oversizedPdfIsRejected() throws Exception {
        File big = tempDir.resolve("big.pdf").toFile();
        try (RandomAccessFile raf = new RandomAccessFile(big, "rw")) {
            raf.setLength(PdfService.MAX_BYTES + 1);
        }
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> pdfService.readPdf(big));
        assertTrue(e.getMessage().startsWith("The PDF is too large"));
    }

    @Test
    void pdfBytesAreReturned() throws Exception {
        byte[] pdf = TestData.pdfBytes();
        File file = Files.write(tempDir.resolve("notes.pdf"), pdf).toFile();
        assertArrayEquals(pdf, pdfService.readPdf(file));
    }
}
