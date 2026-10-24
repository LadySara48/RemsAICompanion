package org.hearlov.remscompanion;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.hearlov.remscompanion.api.*;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
final class ConnectionScreen extends MinimalScreen {
    private final Screen parent;
    private final ProfileStore draft = Connections.store.copy();
    private String error = "";
    private int x, w;

    /*
     * Bağlantı ayarları ekranını kurar, geri dönülecek ekranı saklar.
     */
    ConnectionScreen(Screen parent) { super(label("settings")); this.parent = parent; }
    /*
     * Çeviri anahtarına mod önekini ekleyip ekran yazısı yapar.
     */
    private static Component label(String key) { return Component.translatable("screen.rems_ai_companion." + key); }
    /*
     * Sade bir buton ekler.
     */
    private void button(Component label, int bx, int y, int width, Runnable action) {
        addRenderableWidget(new MinimalButton(bx, y, width, 20, label, action));
    }

    /*
     * Etiketli bir yazı kutusu ekler, gizliyse yazılanı yıldızla gösterir (API key için).
     */
    private EditBox field(String key, String value, int y, int limit, java.util.function.Consumer<String> save, boolean secret) {
        EditBox box = new EditBox(font, x, y, w, 18, label(key));
        box.setMaxLength(limit); box.setValue(value); box.setResponder(save);
        if (secret) box.addFormatter((s, cursor) -> FormattedCharSequence.forward("*".repeat(s.length()), Style.EMPTY));
        addRenderableWidget(box);
        return box;
    }

    /*
     * Profil seçme/ekleme/silme, URL, model, API key, sağlayıcı ve web arama ayarlarını kurar.
     * Kaydederken model doluysa isteği bir kere kurmayı deneyip ayarı kontrol eder.
     */
    @Override protected void init() {
        x = Math.max(10, (width - 400) / 2); w = Math.min(400, width - 20);
        AiProfile p = draft.active();
        button(Component.literal("<"), x, 26, 24, () -> { draft.selected = Math.floorMod(draft.selected - 1, draft.profiles.size()); rebuildWidgets(); });
        button(Component.literal(">"), x + 28, 26, 24, () -> { draft.selected = (draft.selected + 1) % draft.profiles.size(); rebuildWidgets(); });
        button(label("add_profile"), x + 56, 26, 70, () -> { draft.profiles.add(new AiProfile()); draft.selected = draft.profiles.size() - 1; rebuildWidgets(); });
        button(label("delete_profile"), x + 130, 26, 70, () -> {
            if (draft.profiles.size() > 1) { draft.profiles.remove(draft.selected); draft.selected = Math.min(draft.selected, draft.profiles.size() - 1); rebuildWidgets(); }
        });
        field("profile_name", p.name, 65, 80, value -> p.name = value, false);
        field("base_url", p.apiUrl, 90, 1024, value -> p.apiUrl = value, false);
        var modelField = field("model", p.model, 115, 200, value -> p.model = value, false); modelField.setWidth(w - 56);
        button(label("models"), x + w - 52, 114, 52, () -> minecraft.gui.setScreen(new ModelsScreen(this, p, model -> p.model = model)));
        field("api_key", p.apiKey, 140, 1024, value -> p.apiKey = value, true);
        button(Component.translatable("screen.rems_ai_companion.choose_provider", p.provider.label), x, 164, w,
                () -> minecraft.gui.setScreen(new ProviderScreen(this, provider -> {
                    if (p.provider != provider || !p.apiUrl.equals(provider.baseUrl)) {
                        p.provider = provider; p.apiUrl = provider.baseUrl;
                        p.model = ""; p.apiKey = ""; p.webSearch = false;
                    }
                })));
        button(label(!p.provider.supportsSearch() ? "search_unavailable" : p.webSearch ? "search_on" : "search_off"),
                x, 188, w / 2 - 2, () -> { if (p.provider.supportsSearch()) { p.webSearch = !p.webSearch; rebuildWidgets(); } });
        button(label("save"), x, height - 26, w / 2 - 2, () -> {
            if (Connections.loadFailed) { error = "config_load_failed"; return; }
            if (!p.model.isBlank()) {
                try { AiClient.request(p, java.util.List.of(new AiMessage("user", "test"))); }
                catch (RuntimeException ex) { error = "invalid_connection"; return; }
            }
            try { draft.save(Connections.PATH); Connections.store = draft; onClose(); }
            catch (java.io.IOException ex) { error = "save_failed"; }
        });
        button(label("cancel"), x + w / 2 + 2, height - 26, w / 2 - 2, this::onClose);
    }

    /*
     * Alan etiketlerini ve alttaki uyarı ya da hata yazısını çizer.
     */
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        g.fill(0, 0, width, height, 0x8012151A);
        g.text(font, title, x, 10, 0xFFF0F3F7);
        String[] keys = {"profile_name", "base_url", "model", "api_key"};
        for (int i = 0; i < keys.length; i++) g.text(font, label(keys[i]), x, 55 + i * 25, 0xFFE0E9F5);
        Component notice = label(Connections.loadFailed ? "config_load_failed" : !error.isEmpty() ? error : "key_storage");
        int noticeY = 187;
        for (var line : font.split(notice, w / 2 - 4)) {
            if (noticeY > 199) break;
            g.text(font, line, x + w / 2 + 2, noticeY, !error.isEmpty() || Connections.loadFailed ? 0xFFFF9090 : 0xFF9AAEC7);
            noticeY += 10;
        }
        super.extractRenderState(g, mx, my, dt);
    }

    /*
     * Bir önceki ekrana geri döner.
     */
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    /*
     * Bu ekran açıkken oyun durmasın diye false döner.
     */
    @Override public boolean isPauseScreen() { return false; }
}



