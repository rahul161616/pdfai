package com.jugger.pdfai.features.question.controller;

import com.jugger.pdfai.constants.ApiConstants;
import com.jugger.pdfai.features.question.dto.QuestionGenerationResponse;
import com.jugger.pdfai.features.question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.QUESTIONS)
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<QuestionGenerationResponse> generateQuestions(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "5") int count
    ) {
        log.info("Received question generation request for file {} with count {}",
                file != null ? file.getOriginalFilename() : null,
                count);

        QuestionGenerationResponse response = questionService.generateQuestions(file, count);

        log.info("Completed question generation for file {} with {} questions",
                response.getFileName(),
                response.getTotalQuestions());
        return ResponseEntity.ok(response);
    }
}
