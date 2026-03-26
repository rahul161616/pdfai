package com.jugger.pdfai.features.question.service;

import com.jugger.pdfai.features.question.dto.QuestionGenerationResponse;
import org.springframework.web.multipart.MultipartFile;

public interface QuestionService {
    QuestionGenerationResponse generateQuestions(MultipartFile file, int count);
}
