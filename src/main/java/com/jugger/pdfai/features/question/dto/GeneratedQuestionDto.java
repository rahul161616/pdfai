package com.jugger.pdfai.features.question.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class GeneratedQuestionDto {
    private final String question;
    private final String answer;
    private final String type;
    private final String difficulty;
    private final Integer chunkIndex;
}
