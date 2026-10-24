package org.hearlov.remscompanion;

import com.google.gson.*;
import org.hearlov.remscompanion.api.AiUsage;
import org.hearlov.remscompanion.settings.*;
import org.hearlov.remscompanion.storage.BackupFiles;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.time.LocalDate;
import java.io.IOException;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
final class AppState {
    static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve("rems_ai_companion");
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    static PerformanceSettings performance = new PerformanceSettings();
    static UsageLedger ledger = new UsageLedger();
    static boolean settingsFailed, usageFailed;
    static {
        try {
            Path file = ROOT.resolve("settings.json");
            if (BackupFiles.exists(file)) performance = BackupFiles.read(file, text -> {
                var value = JSON.fromJson(text, PerformanceSettings.class);
                if (value == null || value.context == null) throw new IOException("Invalid performance settings"); return value;
            });
        } catch (IOException ex) { settingsFailed = true; }
        try {
            Path file = ROOT.resolve("usage.json");
            if (BackupFiles.exists(file)) ledger = BackupFiles.read(file, text -> {
                var value = JSON.fromJson(text, UsageLedger.class);
                if (value == null || value.days == null || value.lifetime == null) throw new IOException("Invalid usage");
                value.validate();
                for (var entry : value.days.entrySet()) { LocalDate.parse(entry.getKey()); if (entry.getValue() == null) throw new IOException("Invalid daily usage"); }
                return value;
            });
            ledger.prune(LocalDate.now());
        } catch (IOException ex) { usageFailed = true; }
    }

    /*
     * Performans ayarlarını settings.json'a yazar.
     * Dosya önceden okunamadıysa üstüne yazmaz, false döner.
     */
    static boolean saveSettings() {
        if (settingsFailed) return false;
        try { BackupFiles.write(ROOT.resolve("settings.json"), JSON.toJson(performance)); return true; }
        catch (IOException ex) { return false; }
    }

    /*
     * Bir isteğin token ve maliyet kullanımını deftere işler,
     * usage.json'u da arka planda kaydeder.
     */
    static void usage(AiUsage value) {
        if (usageFailed) return;
        ledger.record(LocalDate.now(), value);
        String snapshot = JSON.toJson(ledger);
        ChatSession.enqueue(() -> { try { BackupFiles.write(ROOT.resolve("usage.json"), snapshot); }
            catch (IOException ex) { usageFailed = true; } });
    }
}
