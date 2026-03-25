package com.jugger.pdfai.features.document.controlller;

import com.jugger.pdfai.constants.ApiConstants;
import com.jugger.pdfai.features.document.dto.DocumentChunkResponse;
import com.jugger.pdfai.features.document.dto.DocumentExtractResponse;
import com.jugger.pdfai.features.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.print.Doc;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.DOCUMENTS)
public class DocumentController {

    private final DocumentService documentService;
    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentExtractResponse> extractDocumentText(@RequestPart("file")MultipartFile file) {

        DocumentExtractResponse response = documentService.extractText(file);
        if (response != null) {
            return ResponseEntity.ok(response);
        }
        return null;
    }
    @PostMapping(value = "/chunks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentChunkResponse> extractAndChunk(
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(documentService.extractAndChunk(file));
    }
}
