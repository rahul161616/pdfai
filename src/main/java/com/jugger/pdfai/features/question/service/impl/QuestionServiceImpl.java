package com.jugger.pdfai.features.question.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jugger.pdfai.features.document.dto.ExtractedDocumentData;
import com.jugger.pdfai.features.document.util.PdfTextExtractor;
import com.jugger.pdfai.features.document.util.TextChunker;
import com.jugger.pdfai.features.document.util.TextCleaner;
import com.jugger.pdfai.features.question.dto.GeneratedQuestionDto;
import com.jugger.pdfai.features.question.dto.QuestionGenerationResponse;
import com.jugger.pdfai.features.question.service.QuestionService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
public class QuestionServiceImpl implements QuestionService {

    private final PdfTextExtractor pdfTextExtractor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuestionServiceImpl(PdfTextExtractor pdfTextExtractor) {
        this.pdfTextExtractor = pdfTextExtractor;
    }

    @Value("${ai.base-url:https://integrate.api.nvidia.com}")
    private String aiBaseUrl;

    @Value("${ai.api-key:}")
    private String aiApiKey;

    @Value("${ai.model:meta/llama-3.1-8b-instruct}")
    private String aiModel;

    @Value("${ai.timeout-seconds:60}")
    private long aiTimeoutSeconds;

    @Override
    public QuestionGenerationResponse generateQuestions(MultipartFile file, int count) {
        validateRequest(file, count);

        try {
            ExtractedDocumentData extractedDocumentData = pdfTextExtractor.extract(file);
            String cleanedText = TextCleaner.clean(extractedDocumentData.extractedText());
            List<String> chunks = TextChunker.chunk(cleanedText, 1000, 200);

            if (cleanedText.isBlank()) {
                throw new IllegalArgumentException("The uploaded PDF does not contain readable text");
            }

            List<GeneratedQuestionDto> questions = generateWithAi(cleanedText, chunks, count);
            if (questions.isEmpty()) {
                questions = generateFallbackQuestions(cleanedText, chunks, count);
            }

            return QuestionGenerationResponse.builder()
                    .fileName(extractedDocumentData.fileName())
                    .totalPages(extractedDocumentData.totalPages())
                    .totalQuestions(questions.size())
                    .questions(questions)
                    .build();
        } catch (IOException exception) {
            log.error("Failed to generate questions from PDF file {}", file.getOriginalFilename(), exception);
            throw new RuntimeException("Failed to generate questions from PDF file", exception);
        }
    }

    private List<GeneratedQuestionDto> generateWithAi(String cleanedText, List<String> chunks, int count) {
        if (aiApiKey == null || aiApiKey.isBlank()) {
            log.warn("AI_API_KEY is not configured. Falling back to local question generation.");
            return List.of();
        }

        try {
            String prompt = buildPrompt(cleanedText, chunks, count);
            WebClient webClient = WebClient.builder()
                    .baseUrl(aiBaseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiApiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", aiModel);
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 1600);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "You generate study questions from PDFs. Return valid JSON only."),
                    Map.of("role", "user", "content", prompt)
            ));

            String responseBody = webClient.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(aiTimeoutSeconds));

            return parseQuestionsFromAiResponse(responseBody, count);
        } catch (Exception exception) {
            log.error("AI question generation failed. Falling back to local generation.", exception);
            return List.of();
        }
    }

    private List<GeneratedQuestionDto> parseQuestionsFromAiResponse(String responseBody, int count) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.isNull()) {
            return List.of();
        }

        String content = sanitizeJson(contentNode.asText());
        JsonNode parsed = objectMapper.readTree(content);
        JsonNode questionsNode = parsed.has("questions") ? parsed.get("questions") : parsed;
        if (!questionsNode.isArray()) {
            return List.of();
        }

        List<GeneratedQuestionDto> questions = new ArrayList<>();
        for (JsonNode questionNode : questionsNode) {
            String question = questionNode.path("question").asText(null);
            String answer = questionNode.path("answer").asText(null);
            if (question == null || answer == null) {
                continue;
            }

            questions.add(GeneratedQuestionDto.builder()
                    .question(question)
                    .answer(answer)
                    .type(questionNode.path("type").asText("summary"))
                    .difficulty(questionNode.path("difficulty").asText("medium"))
                    .chunkIndex(questionNode.path("chunkIndex").isInt() ? questionNode.path("chunkIndex").asInt() : null)
                    .build());
        }

        return questions.stream()
                .limit(count)
                .collect(Collectors.toList());
    }

    private String sanitizeJson(String content) {
        String sanitized = content.trim();
        if (sanitized.startsWith("```")) {
            sanitized = sanitized.replaceFirst("^```json", "");
            sanitized = sanitized.replaceFirst("^```", "");
            sanitized = sanitized.replaceFirst("```$", "");
        }
        return sanitized.trim();
    }

    private String buildPrompt(String cleanedText, List<String> chunks, int count) {
        String excerpt = buildExcerpt(cleanedText, chunks);
        return "Generate exactly " + count + " study questions and answers from the provided PDF content. " +
                "Return JSON only in this format: {\"questions\":[{\"question\":\"...\",\"answer\":\"...\",\"type\":\"concept\",\"difficulty\":\"medium\",\"chunkIndex\":1}]}. " +
                "Use concise answers. type must be one of concept, detail, summary, review. difficulty must be one of easy, medium, hard. " +
                "Base the questions only on the supplied text.\n\nDocument text:\n" + excerpt;
    }

    private String buildExcerpt(String cleanedText, List<String> chunks) {
        if (chunks.isEmpty()) {
            return truncate(cleanedText, 6000);
        }

        return chunks.stream()
                .limit(6)
                .map(chunk -> truncate(chunk, 1200))
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private List<GeneratedQuestionDto> generateFallbackQuestions(String cleanedText, List<String> chunks, int count) {
        List<String> sourceChunks = chunks.isEmpty() ? List.of(cleanedText) : chunks;
        List<GeneratedQuestionDto> questions = new ArrayList<>();
        String[] types = {"summary", "concept", "detail", "review"};
        String[] difficulties = {"easy", "medium", "medium", "hard"};

        for (int i = 0; i < sourceChunks.size() && questions.size() < count; i++) {
            String chunk = sourceChunks.get(i).trim();
            if (chunk.isBlank()) {
                continue;
            }

            String answer = firstUsefulSentence(chunk);
            String question = buildFallbackQuestion(chunk, i + 1, questions.size() + 1);
            questions.add(GeneratedQuestionDto.builder()
                    .question(question)
                    .answer(answer)
                    .type(types[questions.size() % types.length])
                    .difficulty(difficulties[questions.size() % difficulties.length])
                    .chunkIndex(i + 1)
                    .build());
        }

        while (questions.size() < count) {
            int questionNumber = questions.size() + 1;
            questions.add(GeneratedQuestionDto.builder()
                    .question("What is one important idea highlighted in the document? (Item " + questionNumber + ")")
                    .answer(firstUsefulSentence(cleanedText))
                    .type("summary")
                    .difficulty("easy")
                    .chunkIndex(1)
                    .build());
        }

        return questions;
    }

    private String buildFallbackQuestion(String chunk, int chunkIndex, int questionNumber) {
        String topic = extractTopic(chunk);
        return switch (questionNumber % 4) {
            case 1 -> "What is the main point discussed in chunk " + chunkIndex + " about " + topic + "?";
            case 2 -> "Which detail from chunk " + chunkIndex + " should a reader remember about " + topic + "?";
            case 3 -> "How would you summarize the section in chunk " + chunkIndex + " covering " + topic + "?";
            default -> "What review question can be answered from chunk " + chunkIndex + " on " + topic + "?";
        };
    }

    private String extractTopic(String chunk) {
        String cleaned = chunk.replaceAll("[^A-Za-z0-9 ]", " ").trim();
        if (cleaned.isBlank()) {
            return "the document";
        }

        String[] words = cleaned.split("\\s+");
        return truncate(String.join(" ", java.util.Arrays.asList(words).subList(0, Math.min(words.length, 6))), 60).toLowerCase(Locale.ROOT);
    }

    private String firstUsefulSentence(String text) {
        String[] sentences = text.split("(?<=[.!?])\\s+");
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() >= 20) {
                return truncate(trimmed, 240);
            }
        }
        return truncate(text.trim(), 240);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private void validateRequest(MultipartFile file, int count) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF file is required");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }

        if (count < 1 || count > 10) {
            throw new IllegalArgumentException("Question count must be between 1 and 10");
        }
    }
}



