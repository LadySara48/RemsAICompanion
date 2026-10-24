package org.hearlov.remscompanion.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
// loader'dan bağımsız json kayıt, klasörü çağıran veriyor
public final class ChatStore {
    public static final int MAX_USER_MESSAGES = 100;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path directory;
    private final Set<UUID> protectedFiles = new HashSet<>();

    public ChatStore(Path directory) { this.directory = directory; }

    public record Loaded(List<ChatRecord> chats, UUID selected, int unreadableFiles) {}

    // 1.18'de gson 2.8 geliyor, record'u okuyamıyor. o yüzden düz class
    private static final class Index {
        int schemaVersion;
        UUID selected;
        Index(int schemaVersion, UUID selected) { this.schemaVersion = schemaVersion; this.selected = selected; }
    }

    public Loaded load() throws IOException {
        List<ChatRecord> chats = new ArrayList<>(); int unreadable = 0;
        UUID selected = null;

        protectedFiles.clear();
        Set<String> names = new TreeSet<>();
        Path backupDirectory = BackupFiles.backup(directory.resolve("index.json")).getParent();
        for (Path source : List.of(directory, backupDirectory)) {
            if (!Files.isDirectory(source)) continue;
            try (var files = Files.newDirectoryStream(source, "*.json")) {
                for (Path file : files) names.add(file.getFileName().toString());
            }
        }

        for (String name : names) {
                Path file = directory.resolve(name);
                if (file.getFileName().toString().equals("index.json")) continue;
                UUID id;
                try { id = UUID.fromString(file.getFileName().toString().replaceFirst("\\.json$", "")); }
                catch (IllegalArgumentException ignored) { continue; }
                try {
                    ChatRecord chat = BackupFiles.read(file, text -> { ChatRecord c = JSON.fromJson(text, ChatRecord.class); validate(c);
                        if (!id.equals(c.id)) throw new IOException("Chat id mismatch"); return c; });
                    chats.add(chat);
                } catch (IOException | RuntimeException ex) { protectedFiles.add(id); unreadable++; }
        }

        Path index = directory.resolve("index.json");
        if (BackupFiles.exists(index)) {
            try {
                Index saved = BackupFiles.read(index, text -> {
                    Index value = JSON.fromJson(text, Index.class); if (value == null || value.schemaVersion != 1) throw new IOException("Invalid chat index"); return value;
                });
                selected = saved.selected;
            } catch (IOException | RuntimeException ex) { unreadable++; }
        }

        chats.sort(Comparator.comparing((ChatRecord c) -> !c.pinned).thenComparingInt(c -> c.number).thenComparing(c -> c.id));
        return new Loaded(chats, selected, unreadable);
    }

    public void save(ChatRecord chat) throws IOException {
        validate(chat);
        if (protectedFiles.contains(chat.id)) throw new IOException("Unreadable chat is protected"); // okunamayan sohbetin üstüne yazmayalım, yoksa tamamen gider
        atomicWrite(directory.resolve(chat.id + ".json"), JSON.toJson(chat));
    }

    public void select(UUID selected) throws IOException {
        atomicWrite(directory.resolve("index.json"), JSON.toJson(new Index(1, selected)));
    }

    public void delete(UUID id) throws IOException {
        if (protectedFiles.contains(id)) throw new IOException("Unreadable chat is protected");
        Files.deleteIfExists(directory.resolve(id + ".json"));
        Files.deleteIfExists(BackupFiles.backup(directory.resolve(id + ".json")));
    }

    private void atomicWrite(Path path, String content) throws IOException {
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "chat-", ".tmp");
        try {
            Files.writeString(temporary, content);
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    public static void validate(ChatRecord chat) throws IOException {
        if (chat != null && (chat.title == null || chat.title.codePointCount(0, chat.title.length()) > ChatTitles.MAX_LENGTH))
            throw new IOException("Invalid title");
        if (chat == null || chat.schemaVersion != 1 || chat.id == null || chat.number < 1
                || chat.profileId == null || chat.promptId == null || chat.summary == null || chat.summary.length() > 12000
                || chat.summaryThrough < 0 || chat.messages != null && chat.summaryThrough > chat.messages.size() || chat.draft == null || chat.draft.length() > 4096 || chat.heldDrafts == null
                || chat.heldDrafts.stream().anyMatch(d -> d == null || d.length() > 4096) || chat.messages == null || chat.sources == null
                || chat.messages.size() > MAX_USER_MESSAGES * 2) throw new IOException("Invalid chat");

        long users = 0;
        for (ChatRecord.Message message : chat.messages) {
            if (message == null || !Set.of("user", "assistant").contains(message.role())
                    || message.text() == null || message.text().length() > 1_000_000 || message.alternatives == null
                    || message.alternatives.size() > 4 || message.alternativeSources == null || message.alternativeSources.size() > 4
                    || message.alternativeSources.stream().anyMatch(s -> s == null || s.stream().anyMatch(Objects::isNull))
                    || message.variant < 0 || !message.alternatives.isEmpty() && message.variant >= message.alternatives.size()
                    || message.regenerations < 0 || message.regenerations > 3
                    || message.alternatives.stream().anyMatch(t -> t == null || t.length() > 1_000_000)) throw new IOException("Invalid message");
            if (message.role().equals("user")) users++; // limit sadece kullanıcı mesajlarını sayıyor
        }

        if (users > MAX_USER_MESSAGES || chat.sources.stream().anyMatch(Objects::isNull)) throw new IOException("Invalid chat");
    }
}
