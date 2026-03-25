package com.jugger.pdfai.features.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
public class DocumentExtractResponse {
    private final String fileName;
    private final Integer totalPages;
    private final String extractedText;
}
