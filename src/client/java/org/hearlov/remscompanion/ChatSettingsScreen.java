package org.hearlov.remscompanion;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.hearlov.remscompanion.storage.ChatTitles;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
final class ChatSettingsScreen extends MinimalScreen {
    private final Screen parent;
    private final ChatSession session;
    private EditBox name;
    private boolean pinned, confirmingDelete, deleting;
    private String profileId, promptId, error = "";
    private int x, w;
    /*
     * Sohbet ayarları ekranını kurar, sohbetin şu anki değerlerini alır.
     */
    ChatSettingsScreen(Screen parent, ChatSession session) {
        super(label("chat_settings")); this.parent = parent; this.session = session;
        pinned = session.pinned; profileId = session.profile().id; promptId = session.resolvedPrompt();
    }

    /*
     * Çeviri anahtarına mod önekini ekleyip ekran yazısı yapar.
     */
    static Component label(String key) { return Component.translatable("screen.rems_ai_companion." + key); }
    /*
     * İpucu yazılı bir buton ekler, silme sürerken pasif kalır.
     */
    private void button(Component text, int bx, int y, int bw, Runnable action) {
        var button = new MinimalButton(bx, y, bw, 20, text, action); button.active = !deleting;
        button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(text)); addRenderableWidget(button);
    }

    /*
     * İsim kutusu, sabitleme, özet, API, prompt, sil ve kaydet butonlarını yerleştirir.
     */
    @Override protected void init() {
        x = Math.max(10, (width - 420) / 2); w = Math.min(420, width - 20);
        String previous = name == null ? session.title : name.getValue();
        name = new EditBox(font, x, 40, w, 20, label("chat_name"));
        name.setMaxLength(ChatTitles.MAX_LENGTH); name.setValue(previous); name.active = !deleting;
        name.setHint(label("chat_name")); addRenderableWidget(name);
        button(label(pinned ? "pinned_on" : "pinned_off"), x, 68, w / 2 - 2, () -> { pinned = !pinned; rebuildWidgets(); });
        button(label("summary"), x + w / 2 + 2, 68, w / 2 - 2, () -> {
            apply(); minecraft.gui.setScreen(new SummaryScreen(this, session));
        });
        button(Component.literal("API: " + Connections.store.resolve(profileId).name), x, 96, w, () -> {
            var profiles = Connections.store.profiles; int index = 0;
            for (int i = 0; i < profiles.size(); i++) if (profiles.get(i).id.equals(profileId)) index = i;
            profileId = profiles.get((index + 1) % profiles.size()).id; rebuildWidgets();
        });
        var promptView = Prompts.store.copy(); promptView.selected = promptId;
        button(Component.literal("Prompt: " + promptView.name()), x, 124, w, () -> {
            var ids = new java.util.ArrayList<String>(); ids.add("default"); Prompts.store.prompts.forEach(p -> ids.add(p.id));
            promptId = ids.get(Math.floorMod(ids.indexOf(promptId) + 1, ids.size())); rebuildWidgets();
        });
        button(label("save"), x, 152, w / 2 - 2, () -> { apply(); onClose(); });
        button(label(confirmingDelete ? "confirm_delete" : "delete_chat"), x + w / 2 + 2, 152, w / 2 - 2, () -> {
            if (!confirmingDelete) { confirmingDelete = true; rebuildWidgets(); return; }
            deleting = true; rebuildWidgets();
            ChatSession.delete(session, success -> {
                deleting = false;
                if (success) minecraft.gui.setScreen(new CompanionScreen());
                else { error = "chat_delete_failed"; confirmingDelete = false; rebuildWidgets(); }
            });
        });
        if (!session.heldDrafts.isEmpty()) button(label("other_draft"), x, 180, w / 2 - 2, () -> {
            String current = session.draft; session.draft = session.heldDrafts.removeFirst();
            if (!current.isBlank()) session.heldDrafts.add(current); session.changed(); session.save(); onClose();
        });
        var path = Connections.PATH.getParent().resolve("chats").toAbsolutePath().toString();
        var folder = new MinimalButton(x + w / 2 + 2, 180, w / 2 - 2, 20, label("history_path"), () -> minecraft.keyboardHandler.setClipboard(path));
        folder.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(path))); folder.active = !deleting; addRenderableWidget(folder);
        button(label("back"), x, height - 26, w, this::onClose);
    }

    /*
     * Ekrandaki seçimleri sohbete uygular, kaydeder ve listeyi yeniden sıralar.
     */
    private void apply() {
        if (!name.getValue().isBlank()) session.rename(name.getValue());
        session.profileId = profileId; session.promptId = promptId; session.pinned = pinned;
        session.changed(); session.save(); ChatSession.sort();
    }

    /*
     * Arka planı, başlığı ve varsa hata yazısını çizer.
     */
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        g.fill(0, 0, width, height, 0x8012151A); g.text(font, title, x, 15, 0xFFF0F3F7);
        if (!error.isEmpty()) g.text(font, label(error), x, height - 40, 0xFFFF9090);
        super.extractRenderState(g, mx, my, dt);
    }

    /*
     * Silme bitmeden çıkmaz, sonra ana sohbet ekranına döner.
     */
    @Override public void onClose() { if (!deleting) minecraft.gui.setScreen(new CompanionScreen()); }
}
