package com.jugger.pdfai.features.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class DocumentChunkResponse {

    private final String fileName;
    private final Integer totalPages;
    private final Integer totalChunks;
    private final List<ChunkData> chunks;

    @Getter
    @AllArgsConstructor
    public static class ChunkData {
        private final Integer index;
        private final String content;
    }
}