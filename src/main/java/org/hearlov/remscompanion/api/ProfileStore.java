package org.hearlov.remscompanion.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
public final class ProfileStore {
    public int configVersion = 1;
    public int selected;
    public List<AiProfile> profiles = new ArrayList<>();

    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();

    public static ProfileStore load(Path path) throws IOException {
        if (!org.hearlov.remscompanion.storage.BackupFiles.exists(path)) { ProfileStore store = new ProfileStore(); store.profiles.add(new AiProfile()); return store; } // dosya yoksa tek boş profille başla

        ProfileStore loaded = org.hearlov.remscompanion.storage.BackupFiles.read(path, text -> {
            ProfileStore store = JSON.fromJson(text, ProfileStore.class);
            if (store == null || store.configVersion != 1 || store.profiles == null || store.profiles.isEmpty()) throw new IOException("Invalid config");
            for (AiProfile p : store.profiles) {
                if (p == null || p.provider == null || p.name == null || p.apiUrl == null || p.apiKey == null || p.model == null || p.systemPrompt == null)
                    throw new IOException("Invalid profile");
            }

            var ids = new java.util.HashSet<String>();
            for (AiProfile p : store.profiles) if (p.id == null || p.id.isBlank() || !ids.add(p.id)) throw new IOException("Invalid profile identity"); // id boş ya da tekrarlıysa dosyayı bozuk sayıyorum
            store.selected = Math.max(0, Math.min(store.selected, store.profiles.size() - 1));
            return store;
        });

        // eski dosyada id üretildiyse bi kere kaydediyorum, yoksa restart sonrası sohbetler profilini bulamıyor
        String original = Files.readString(path);
        var entries = com.google.gson.JsonParser.parseString(original).getAsJsonObject().getAsJsonArray("profiles");
        boolean migrated = false;
        for (var entry : entries) if (!entry.getAsJsonObject().has("id")) migrated = true;
        if (migrated) loaded.save(path);
        return loaded;
    }

    public AiProfile active() { return profiles.get(selected); }
    public AiProfile resolve(String id) { return profiles.stream().filter(p -> p.id.equals(id)).findFirst().orElse(active()); }
    public ProfileStore copy() { return JSON.fromJson(JSON.toJson(this), ProfileStore.class); }

    public void save(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "profiles-", ".tmp");
        try {
            Files.writeString(temporary, JSON.toJson(this));
            try { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
