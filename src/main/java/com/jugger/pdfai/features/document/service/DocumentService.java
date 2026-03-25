package com.jugger.pdfai.features.document.service;

import com.jugger.pdfai.features.document.dto.DocumentChunkResponse;
import com.jugger.pdfai.features.document.dto.DocumentExtractResponse;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
    DocumentExtractResponse extractText(MultipartFile file);
    DocumentChunkResponse extractAndChunk(MultipartFile file);
}
