package org.hearlov.remscompanion.api.fabric;

import net.fabricmc.loader.api.FabricLoader;
import org.hearlov.remscompanion.storage.ChatStore;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
/** Fabric only chooses the directory; JSON storage itself works with any loader. */
public final class FabricChatStorage {
    /*
     * Sadece static kullanılsın diye boş private kurucu.
     */
    private FabricChatStorage() {}
    /*
     * Fabric config klasöründeki chats dizinini kullanan bir ChatStore oluşturur.
     */
    public static ChatStore create() {
        return new ChatStore(FabricLoader.getInstance().getConfigDir().resolve("rems_ai_companion/chats"));
    }
}
