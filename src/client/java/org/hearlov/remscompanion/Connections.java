package org.hearlov.remscompanion;

import net.fabricmc.loader.api.FabricLoader;
import org.hearlov.remscompanion.api.ProfileStore;
import java.nio.file.Path;
import java.io.IOException;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
final class Connections {
    static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("rems_ai_companion/profiles.json");
    static ProfileStore store;
    static boolean loadFailed;
    static {
        try { store = ProfileStore.load(PATH); }
        catch (IOException ex) {
            store = new ProfileStore(); store.profiles.add(new org.hearlov.remscompanion.api.AiProfile());
            loadFailed = true;
        }
    }
}
