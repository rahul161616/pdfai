package com.jugger.pdfai.features.document.dto;

public record ExtractedDocumentData(
        String fileName,
        Integer totalPages,
        String extractedText
) {}