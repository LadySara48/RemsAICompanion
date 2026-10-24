package org.hearlov.remscompanion.chat;

import org.hearlov.remscompanion.api.AiMessage;
import java.util.*;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
public final class SummaryContext {
    private SummaryContext() {}

    public static String build(List<AiMessage> messages, String previous, int limit) {
        String memory = previous.isBlank() ? "" : "Previous summary:\n" + previous + "\n\n";
        if (memory.length() >= limit) throw new IllegalArgumentException("context_too_large"); // eski özet bile sığmıyorsa yapacak bi şey yok
        int remaining = limit - memory.length(); LinkedList<String> parts = new LinkedList<>();

        for (int i = messages.size() - 1; i >= 0 && remaining > 0; i--) { // sondan geriye gidiyorum ki en son konuşma kesin sığsın
            String text = messages.get(i).role() + ": " + messages.get(i).content() + "\n";
            if (text.length() > remaining) text = text.substring(text.length() - remaining);
            parts.addFirst(text); remaining -= text.length();
        }

        return memory + String.join("", parts);
    }
}
