package org.hearlov.remscompanion.chat;

import org.hearlov.remscompanion.storage.ChatRecord;
import java.util.*;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
public final class AnswerVersions {
    private AnswerVersions() {}

    public static boolean begin(ChatRecord.Message message, Collection<String> sources) {
        if (!message.complete || message.regenerations >= 3) return false; // en fazla 3 yenileme
        if (message.alternatives.isEmpty()) { message.alternatives.add(message.text); message.alternativeSources.add(new ArrayList<>(sources)); message.variant = 0; } // ilk yenilemede eski cevabı alternatiflere atıyorum, kaybolmasın
        message.regenerations++; message.text = ""; message.complete = false; return true;
    }

    public static void finish(ChatRecord.Message message, String text, Collection<String> sources) {
        message.alternatives.add(text); message.alternativeSources.add(new ArrayList<>(sources));
        message.variant = message.alternatives.size() - 1; message.text = text; message.complete = true;
    }

    public static void rollback(ChatRecord.Message message) {
        if (message.alternatives.isEmpty()) return;
        message.variant = Math.max(0, Math.min(message.variant, message.alternatives.size() - 1));
        message.text = message.alternatives.get(message.variant); message.complete = true;
    }

    public static void choose(ChatRecord.Message message, int delta) {
        if (!message.complete || message.alternatives.isEmpty()) return;
        message.variant = Math.floorMod(message.variant + delta, message.alternatives.size()); rollback(message);
    }

    public static List<String> sources(ChatRecord.Message message) {
        return message.variant < message.alternativeSources.size() ? message.alternativeSources.get(message.variant) : List.of();
    }
}
