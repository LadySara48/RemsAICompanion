package org.hearlov.remscompanion.chat;

import java.util.*;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
// istek patlarsa mesaj editöre geri dönüyor, beklerken yazılanı da silmiyorum
public final class DraftRecovery {
    private DraftRecovery() {}
    public record Result(String active, List<String> held) {}

    public static Result restore(String failedMessage, String currentDraft, List<String> heldDrafts) {
        List<String> held = new ArrayList<>(heldDrafts);
        if (!currentDraft.isBlank() && !currentDraft.equals(failedMessage)) held.add(currentDraft); // beklerken yazılan taslağı da kaybetmeyelim
        return new Result(failedMessage, List.copyOf(held));
    }
}
