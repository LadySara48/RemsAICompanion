package org.hearlov.remscompanion;

import net.minecraft.client.Minecraft;
import org.hearlov.remscompanion.api.*;
import org.hearlov.remscompanion.chat.*;
import org.hearlov.remscompanion.storage.ContextWindow;
import java.util.*;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
final class ChatRequests {
    /*
     * Sohbete bağlı profili kopyalayıp ayarları ve promptu kontrol eder,
     * sonra oyun bilgisiyle birlikte sistem promptunu hazırlar.
     */
    static AiProfile profile(ChatSession session) {
        if (Connections.loadFailed) throw new IllegalArgumentException("config_load_failed");
        if (Prompts.loadFailed) throw new IllegalArgumentException("prompt_load_failed");
        AiProfile profile = session.profile().copy();
        if (profile.model.isBlank()) throw new IllegalArgumentException("configure_first");
        var prompt = Prompts.store.copy(); prompt.selected = session.resolvedPrompt();
        profile.systemPrompt = Prompts.compose(prompt, Minecraft.getInstance()); return profile;
    }

    /*
     * Özetten sonraki tamamlanmış mesajları API'ye gidecek geçmiş listesine çevirir.
     */
    static List<AiMessage> history(ChatSession session, int end) {
        List<AiMessage> history = new ArrayList<>();
        for (int i = Math.min(session.summaryThrough, end); i < end; i++) {
            var m = session.messages.get(i); if (m.complete) history.add(new AiMessage(m.role, m.text));
        }
        return history;
    }

    /*
     * Mesajı gönderir ya da son cevabı yeniden ürettirir.
     * Cevap akışını dinleyip metni, kaynakları ve hataları sohbete işler.
     */
    static boolean send(ChatSession session, String value, boolean regenerate) {
        Minecraft client = Minecraft.getInstance();
        if (session.busy) { if (session.call != null) session.call.cancel(); return false; }
        if (value.isBlank()) return false;
        if (!regenerate && session.atLimit()) { session.error = "chat_limit"; return false; }
        int end = session.messages.size();
        if (regenerate && (end < 2 || !session.messages.getLast().role.equals("assistant") || !session.messages.getLast().complete
                || session.messages.getLast().regenerations >= 3 || end - 2 < session.summaryThrough)) { session.error = "regenerate_unavailable"; return false; }
        AiProfile profile;
        List<AiMessage> history;
        try {
            profile = profile(session);
            int budget = AppState.performance.context.characters;
            String memory = session.summary.isBlank() ? "" : "Previous conversation summary:\n" + session.summary;
            history = ContextWindow.build(history(session, regenerate ? end - 2 : end), value, profile.systemPrompt,
                    budget - (memory.isEmpty() ? 0 : memory.length() + 12));
            if (!memory.isEmpty()) { var combined = new ArrayList<AiMessage>(); combined.add(new AiMessage("user", memory));
                combined.add(new AiMessage("assistant", "Understood.")); combined.addAll(history); history = combined; }
            AiClient.request(profile, history);
        } catch (RuntimeException ex) { session.error = ex.getMessage() != null && ex.getMessage().startsWith("prompt") ? "prompt_too_large" :
                Set.of("configure_first", "config_load_failed", "prompt_load_failed", "context_too_large").contains(String.valueOf(ex.getMessage())) ? ex.getMessage() : "invalid_connection"; return false; }
        session.bind();
        ChatSession.Message user, answer;
        if (regenerate) {
            user = session.messages.get(end - 2); answer = session.messages.getLast();
            if (!AnswerVersions.begin(answer, session.sources)) return false;
        } else {
            user = new ChatSession.Message("user", value, true); answer = new ChatSession.Message("assistant", "", false);
            session.messages.add(user); session.messages.add(answer); session.draft = "";
        }
        session.busy = true; session.summaryBusy = false; session.error = ""; session.sources.clear(); session.changed(); session.save();
        CompanionVoices.stop(client); CompanionVoices.BEHAVIOUR.sent();
        session.call = AiClient.stream(profile, history, new AiClient.Listener() {
            /*
             * Gelen her metin parçasını oyun thread'inde cevaba ekler.
             */
            public void text(String delta) { client.execute(() -> { if (!session.deleted) { answer.text += delta; session.changed(); } }); }
            /*
             * Cevapta gelen kaynak linkini sohbetin kaynak listesine ekler.
             */
            public void source(String url) { client.execute(() -> { if (!session.deleted) session.sources.add(url); }); }
            /*
             * Gelen kullanım bilgisini istatistiğe yazar.
             */
            public void usage(AiUsage usage) { client.execute(() -> AppState.usage(usage)); }
            /*
             * Cevap bitince sohbeti tamamlanmış sayar, başlık verir ve kaydeder.
             */
            public void done() { client.execute(() -> {
                if (session.deleted) return;
                if (regenerate) AnswerVersions.finish(answer, answer.text, session.sources); else answer.complete = true;
                session.busy = false; session.call = null; session.autoTitle(answer.text); session.changed(); session.save();
                CompanionVoices.BEHAVIOUR.answered();
            }); }
            /*
             * Hata gelirse mesajı taslağa geri koyar ya da yenilemeyi geri alır, hatayı gösterir.
             */
            public void error(String code) { client.execute(() -> {
                if (session.deleted) return; session.busy = false; session.call = null;
                if (regenerate) { AnswerVersions.rollback(answer); session.sources.clear(); session.sources.addAll(AnswerVersions.sources(answer)); }
                else {
                    user.complete = false;
                    if (!code.equals("cancelled")) {
                        var recovered = DraftRecovery.restore(value, session.draft, session.heldDrafts);
                        session.heldDrafts.clear(); session.heldDrafts.addAll(recovered.held()); session.draft = recovered.active();
                        if (answer.text.isEmpty()) { session.messages.remove(answer); session.messages.remove(user); }
                    }
                }
                session.error = code; session.changed(); session.save();
            }); }
        });
        return true;
    }
}
