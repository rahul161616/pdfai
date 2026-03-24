package com.jugger.pdfai.features.document.util;

public final class TextCleaner {

    private TextCleaner() {
    }

    public static String clean(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        return text
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" +", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}