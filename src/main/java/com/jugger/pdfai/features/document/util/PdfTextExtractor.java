package com.jugger.pdfai.features.document.util;

import com.jugger.pdfai.features.document.dto.ExtractedDocumentData;
import lombok.Builder;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
@Builder
public class PdfTextExtractor {
    public ExtractedDocumentData extract(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            String extractedText = pdfTextStripper.getText(document);

            return new ExtractedDocumentData(
                    file.getOriginalFilename(),
                    document.getNumberOfPages(),
                    extractedText
            );
        }
    }

}
