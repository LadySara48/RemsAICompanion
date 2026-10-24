package org.hearlov.remscompanion.storage;

import org.hearlov.remscompanion.api.AiMessage;
import java.util.*;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
// token tahmini değil, düz karakter bütçesi. biten bi turu asla ortadan bölmüyorum
public final class ContextWindow {
    public static final int MAX_CHARACTERS = 16_000;
    private ContextWindow() {}

    public static List<AiMessage> build(List<AiMessage> history, String next, String systemPrompt) {
        return build(history, next, systemPrompt, MAX_CHARACTERS);
    }

    public static List<AiMessage> build(List<AiMessage> history, String next, String systemPrompt, int characters) {
        if (systemPrompt.length() > org.hearlov.remscompanion.prompt.PromptComposer.MAX_TOTAL)
            throw new IllegalArgumentException("context_too_large");

        int remaining = characters - next.length();
        if (remaining < 0) throw new IllegalArgumentException("context_too_large");
        List<List<AiMessage>> turns = new ArrayList<>();
        for (int i = 0; i + 1 < history.size(); i++) {
            AiMessage user = history.get(i), answer = history.get(i + 1);
            if (user.role().equals("user") && answer.role().equals("assistant")) {
                turns.add(List.of(user, answer)); i++;
            }
        }

        LinkedList<AiMessage> result = new LinkedList<>();
        for (int i = turns.size() - 1; i >= 0; i--) {
            List<AiMessage> turn = turns.get(i);
            int size = turn.get(0).content().length() + turn.get(1).content().length();
            if (size > remaining) break; // bütçe doldu, eskiler girmiyor. turu da bölmüyorum
            remaining -= size; result.addAll(0, turn);
        }

        result.add(new AiMessage("user", next));
        return List.copyOf(result);
    }
}
