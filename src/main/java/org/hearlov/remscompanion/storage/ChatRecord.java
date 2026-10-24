package org.hearlov.remscompanion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
// sadece kaydedilen sohbet verisi. runtime çağrısı, api key, minecraft tipi yok burda
public final class ChatRecord {
    public int schemaVersion = 1;
    public UUID id = UUID.randomUUID();
    public int number;
    public String title = "";
    public boolean manualTitle;
    public boolean autoTitleSet;
    public boolean pinned;
    public String profileId = "", promptId = "";
    public String summary = "";
    public int summaryThrough;
    public List<String> heldDrafts = new ArrayList<>();
    public long createdAt = System.currentTimeMillis();
    public long updatedAt = createdAt;
    public String draft = "";
    public boolean inProgress;
    public List<Message> messages = new ArrayList<>();
    public List<String> sources = new ArrayList<>();

    public static class Message {
        public String role = "", text = "";
        public boolean complete;
        public List<String> alternatives = new ArrayList<>();
        public List<List<String>> alternativeSources = new ArrayList<>();
        public int variant, regenerations;

        public Message() {} // gson için boş ctor lazım
        public Message(String role, String text, boolean complete) { this.role = role; this.text = text; this.complete = complete; }
        public String role() { return role; }
        public String text() { return text; }
        public boolean complete() { return complete; }
        @Override public boolean equals(Object o) { return o instanceof Message m && role.equals(m.role) && text.equals(m.text) && complete == m.complete; }
        @Override public int hashCode() { return java.util.Objects.hash(role, text, complete); }
    }

    public boolean recoverInterrupted() {
        if (!inProgress) return false;
        inProgress = false;

        if (!messages.isEmpty()) {
            int last = messages.size() - 1;
            Message answer = messages.get(last);
            if (answer.role.equals("assistant") && !answer.alternatives.isEmpty() && !answer.complete) {
                org.hearlov.remscompanion.chat.AnswerVersions.rollback(answer);
                sources = new ArrayList<>(org.hearlov.remscompanion.chat.AnswerVersions.sources(answer)); return true;
            }

            if (answer.role().equals("assistant")) {
                messages.set(last, new Message(answer.role(), answer.text(), false)); // yarım kalan cevabı tamamlanmadı diye işaretliyorum
                if (last > 0 && messages.get(last - 1).role().equals("user")) {
                    Message user = messages.get(last - 1);
                    messages.set(last - 1, new Message(user.role(), user.text(), false));
                }
            }
        }

        return true;
    }
}
