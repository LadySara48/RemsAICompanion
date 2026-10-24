package org.hearlov.remscompanion.companion;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
public final class VoiceSettings {
    public enum Language { AUTO, ENGLISH, JAPANESE }

    public boolean enabled = true;
    public int volume = 50;
    public Language language = Language.AUTO;
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();

    public String suffix(String locale) { return language == Language.JAPANESE || (language == Language.AUTO && locale.startsWith("ja")) ? "ja" : "en"; } // otomatikse oyun dili ja ile mi başlıyor ona bakıyorum
    public VoiceSettings copy() { return JSON.fromJson(JSON.toJson(this), VoiceSettings.class); }

    public static VoiceSettings load(Path path) throws IOException {
        if (!org.hearlov.remscompanion.storage.BackupFiles.exists(path)) return new VoiceSettings();
        return org.hearlov.remscompanion.storage.BackupFiles.read(path, text -> {
            VoiceSettings settings = JSON.fromJson(text, VoiceSettings.class);
            if (settings == null || settings.language == null || settings.volume < 0 || settings.volume > 100) throw new IOException("Invalid voice settings");
            return settings;
        });
    }

    public void save(Path path) throws IOException {
        Files.createDirectories(path.getParent()); Path temp = Files.createTempFile(path.getParent(), "voice-", ".tmp");
        try {
            Files.writeString(temp, JSON.toJson(this));
            try { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); } // atomic move yoksa düz taşı
        } finally { Files.deleteIfExists(temp); }
    }
}
