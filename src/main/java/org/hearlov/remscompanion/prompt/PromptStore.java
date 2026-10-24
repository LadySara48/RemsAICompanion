package org.hearlov.remscompanion.prompt;

import com.google.gson.*;
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
public final class PromptStore {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public int schemaVersion = 1;
    public String selected = "default";
    public Variables defaultVariables = new Variables();
    public List<Entry> prompts = new ArrayList<>();

    public static final class Variables {
        public String name = "Rem", age = "Not specified", playerName = "", language = "", behaviour = "";
    }

    public static final class Entry {
        public String id = UUID.randomUUID().toString();
        public String name = "Custom";
        public String template = "";
        public Variables variables = new Variables();
    }

    public Entry activeCustom() { return prompts.stream().filter(p -> p.id.equals(selected)).findFirst().orElse(null); }
    public String template() { Entry p = activeCustom(); return p == null ? DefaultPrompt.TEXT : p.template; } // özel seçili değilse zaten default metin
    public Variables variables() { Entry p = activeCustom(); return p == null ? defaultVariables : p.variables; }
    public String name() { Entry p = activeCustom(); return p == null ? "Default" : p.name; }
    public PromptStore copy() { return JSON.fromJson(JSON.toJson(this), PromptStore.class); }

    public static PromptStore load(Path file) throws IOException {
        if (!org.hearlov.remscompanion.storage.BackupFiles.exists(file)) return new PromptStore();
        return org.hearlov.remscompanion.storage.BackupFiles.read(file, text -> {
            PromptStore result = JSON.fromJson(text, PromptStore.class); validate(result); return result;
        });
    }

    public void save(Path file) throws IOException {
        validate(this); Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), "prompts-", ".tmp");
        try {
            Files.writeString(temp, JSON.toJson(this));
            try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }

    public static void validate(PromptStore store) throws IOException {
        if (store == null || store.schemaVersion != 1 || store.selected == null || store.prompts == null) throw new IOException("Invalid prompts");
        validateVariables(store.defaultVariables);
        Set<String> ids = new HashSet<>(); ids.add("default"); // default'u baştan ekledim, özel prompt bu id'yi alamasın
        for (Entry p : store.prompts) {
            if (p == null || p.id == null || !ids.add(p.id) || p.name == null || p.name.length() > 80
                    || p.template == null || p.template.length() > PromptComposer.MAX_BODY) throw new IOException("Invalid prompt");
            validateVariables(p.variables);
        }

        if (!ids.contains(store.selected)) throw new IOException("Unknown prompt selection");
    }

    private static void validateVariables(Variables v) throws IOException {
        if (v == null || v.name == null || v.age == null || v.playerName == null || v.language == null || v.behaviour == null
                || v.name.length() > 80 || v.age.length() > 40 || v.playerName.length() > 80 || v.language.length() > 80
                || v.behaviour.length() > 2000) throw new IOException("Invalid prompt variables");
    }
}
