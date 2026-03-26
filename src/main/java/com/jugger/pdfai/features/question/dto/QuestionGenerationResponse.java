package com.jugger.pdfai.features.question.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class QuestionGenerationResponse {
    private final String fileName;
    private final Integer totalPages;
    private final Integer totalQuestions;
    private final List<GeneratedQuestionDto> questions;
}
