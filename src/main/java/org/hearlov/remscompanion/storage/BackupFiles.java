package org.hearlov.remscompanion.storage;

import com.google.gson.*;
import java.nio.file.*;
import java.io.IOException;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
// tek bi yedek var, her seferinde üstüne yazılıyor. tarihli arşiv biriktirmiyorum
public final class BackupFiles {
    private BackupFiles() {}
    @FunctionalInterface public interface Parser<T> { T parse(String text) throws IOException; }

    public static Path backup(Path file) {
        Path absolute = file.toAbsolutePath(); Path root = absolute.getParent();
        while (root != null && (root.getFileName() == null || !root.getFileName().toString().equals("rems_ai_companion"))) root = root.getParent(); // rems_ai_companion klasörünü bulana kadar yukarı çık
        return root == null ? absolute.getParent().resolve("backup").resolve(absolute.getFileName())
                : root.resolve("backup").resolve(root.relativize(absolute));
    }

    public static boolean exists(Path file) { return Files.exists(file) || Files.exists(backup(file)); }
    public static <T> T read(Path file, Parser<T> parser) throws IOException {
        try { if (Files.size(file) > 128L * 1024 * 1024) throw new IOException("File too large"); return parser.parse(Files.readString(file)); }
        catch (IOException | RuntimeException ex) {
            try {
                String text = Files.readString(backup(file)); T recovered = parser.parse(text); // ana dosya bozuksa yedeğe bak
                write(file, text); return recovered;
            } catch (IOException | RuntimeException backupFailure) { throw new IOException("No valid primary or backup", ex); }
        }
    }

    public static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent()); Path temp = Files.createTempFile(file.getParent(), "saved-", ".tmp");
        try {
            Files.writeString(temp, text);
            try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }

    public static void mirror(Path root) throws IOException {
        if (!Files.isDirectory(root)) return;
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> !p.startsWith(root.resolve("backup"))).toList()) {
                try {
                    String text = Files.readString(file); JsonObject object = JsonParser.parseString(text).getAsJsonObject();
                    if (object.has("schemaVersion") && object.get("schemaVersion").getAsInt() != 1) continue;
                    if (object.has("configVersion") && object.get("configVersion").getAsInt() != 1) continue;
                    if (file.getParent().getFileName().toString().equals("chats") && !file.getFileName().toString().equals("index.json"))
                        ChatStore.validate(new Gson().fromJson(object, ChatRecord.class));
                    switch (file.getFileName().toString()) {
                        case "profiles.json" -> {
                            var p = new Gson().fromJson(object, org.hearlov.remscompanion.api.ProfileStore.class);
                            if (p.profiles == null || p.profiles.isEmpty() || p.selected < 0 || p.selected >= p.profiles.size()) continue;
                            boolean valid = p.profiles.stream().allMatch(v -> v != null && v.provider != null && v.name != null && v.apiUrl != null && v.apiKey != null && v.model != null);
                            if (!valid) continue;
                        }
                        case "prompts.json" -> org.hearlov.remscompanion.prompt.PromptStore.validate(new Gson().fromJson(object, org.hearlov.remscompanion.prompt.PromptStore.class));
                        case "voice.json" -> {
                            var v = new Gson().fromJson(object, org.hearlov.remscompanion.companion.VoiceSettings.class);
                            if (v.language == null || v.volume < 0 || v.volume > 100) continue;
                        }
                        case "settings.json" -> { if (new Gson().fromJson(object, org.hearlov.remscompanion.settings.PerformanceSettings.class).context == null) continue; }
                        case "usage.json" -> {
                            var v = new Gson().fromJson(object, org.hearlov.remscompanion.settings.UsageLedger.class);
                            if (v.lifetime == null || v.days == null) continue;
                            v.validate();
                        }
                    }

                    write(backup(file), text);
                } catch (IOException | RuntimeException ignored) { /* ana dosya bozuksa sağlam yedeği ezmesin */ }
            }
        }
    }
}
