package com.jugger.pdfai.exception;

public class GeneralNotFoundException extends RuntimeException {

    public GeneralNotFoundException(String message) {
        super(message);
    }

    public GeneralNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
