package com.jugger.pdfai.features.document.service.impl;

import com.jugger.pdfai.features.document.dto.DocumentChunkResponse;
import com.jugger.pdfai.features.document.dto.DocumentExtractResponse;
import com.jugger.pdfai.features.document.dto.ExtractedDocumentData;
import com.jugger.pdfai.features.document.service.DocumentService;
import com.jugger.pdfai.features.document.util.PdfTextExtractor;
import com.jugger.pdfai.features.document.util.TextChunker;
import com.jugger.pdfai.features.document.util.TextCleaner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {
    private final PdfTextExtractor pdfTextExtractor;
    public DocumentExtractResponse extractText(MultipartFile file){
        validateFile(file);

        try {
            ExtractedDocumentData extractedDocumentData = pdfTextExtractor.extract(file);

            return DocumentExtractResponse.builder()
                    .fileName(extractedDocumentData.fileName())
                    .totalPages(extractedDocumentData.totalPages())
                    .extractedText(extractedDocumentData.extractedText())
                    .build();

        } catch (IOException exception) {
            throw new RuntimeException("Failed to extract text from PDF file", exception);
        }
    }

    @Override
    public DocumentChunkResponse extractAndChunk(MultipartFile file) {
        validateFile(file);

        try {
            ExtractedDocumentData extractedDocumentData = pdfTextExtractor.extract(file);
            String cleanedText = TextCleaner.clean(extractedDocumentData.extractedText());

            List<String> rawChunks = TextChunker.chunk(cleanedText, 1000, 200);
            List<DocumentChunkResponse.ChunkData> chunks = new ArrayList<>();

            for (int i = 0; i < rawChunks.size(); i++) {
                chunks.add(new DocumentChunkResponse.ChunkData(i + 1, rawChunks.get(i)));
            }

            return DocumentChunkResponse.builder()
                    .fileName(extractedDocumentData.fileName())
                    .totalPages(extractedDocumentData.totalPages())
                    .totalChunks(chunks.size())
                    .chunks(chunks)
                    .build();

        } catch (IOException exception) {
            throw new RuntimeException("Failed to extract and chunk PDF file", exception);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF file is required");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }
    }

}
