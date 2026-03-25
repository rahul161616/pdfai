package com.jugger.pdfai.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Builder
@Getter
@AllArgsConstructor
public class ErrorResponse {
    private final String message;
    private final HttpStatus status;
    private final String error;
    private final String path;
    private final String timestamp;
}
