package org.hearlov.remscompanion.storage;

import java.util.regex.Pattern;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
public final class ChatTitles {
    public static final int MAX_LENGTH = 24;
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{M}\\p{N}'’_-]*");
    private ChatTitles() {}

    public static String normalize(String title) {
        String value = title.replaceAll("[\\p{Z}\\s]+", " ").strip();
        return value.codePointCount(0, value.length()) <= MAX_LENGTH ? value
                : value.substring(0, value.offsetByCodePoints(0, MAX_LENGTH)).stripTrailing();
    }

    public static String fromAnswer(String answer) {
        var words = WORD.matcher(answer); StringBuilder title = new StringBuilder();
        for (int i = 0; i < 2 && words.find(); i++) { // başlık için ilk iki kelime yeter
            if (!title.isEmpty()) title.append(' ');
            title.append(words.group());
        }

        return normalize(title.toString());
    }
}
