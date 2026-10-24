package org.hearlov.remscompanion;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.hearlov.remscompanion.api.*;
import java.util.*;
import java.util.function.Consumer;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
final class ModelsScreen extends MinimalScreen {
    private final Screen parent; private final AiProfile profile; private final Consumer<String> select;
    private List<String> models = List.of(); private String query = "", error = ""; private int page;
    private EditBox search; private boolean started, loading;
    /*
     * Model listesi ekranını kurar, profili ve seçim fonksiyonunu saklar.
     */
    ModelsScreen(Screen parent, AiProfile profile, Consumer<String> select) { super(SettingsScreen.label("models")); this.parent = parent; this.profile = profile.copy(); this.select = select; }
    /*
     * Arama kutusuna göre modelleri büyük küçük harfe bakmadan süzer.
     */
    private List<String> filtered() { return models.stream().filter(s -> s.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).toList(); }
    /*
     * Arama kutusunu ve sayfalı model butonlarını kurar,
     * ilk açılışta model listesini arka planda çeker.
     */
    @Override protected void init() {
        int x = 10, w = width - 20, perPage = Math.max(1, (height - 115) / 22);
        search = new EditBox(font, x, 34, w, 18, SettingsScreen.label("models")); search.setValue(query);
        search.setResponder(value -> { query = value; page = 0; rebuildWidgets(); setFocused(search); }); addRenderableWidget(search);
        var list = filtered(); page = Math.min(page, Math.max(0, (list.size() - 1) / perPage));
        for (int i = page * perPage; i < Math.min(list.size(), (page + 1) * perPage); i++) {
            String model = list.get(i); var b = new MinimalButton(x, 60 + (i - page * perPage) * 22, w, 20, Component.literal(model), () -> { select.accept(model); onClose(); });
            b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(model))); addRenderableWidget(b);
        }
        var previous = new MinimalButton(x, height - 50, 45, 20, Component.literal("<"), () -> { page--; rebuildWidgets(); }); previous.active = page > 0; addRenderableWidget(previous);
        var next = new MinimalButton(x + w - 45, height - 50, 45, 20, Component.literal(">"), () -> { page++; rebuildWidgets(); }); next.active = (page + 1) * perPage < list.size(); addRenderableWidget(next);
        addRenderableWidget(new MinimalButton(x, height - 26, w, 20, SettingsScreen.label("back"), this::onClose));
        if (!started) {
            started = true; loading = true;
            ModelCatalog.load(profile).whenComplete((result, failure) -> minecraft.execute(() -> {
                loading = false; if (failure != null) {
                    Throwable cause = failure; while (cause.getCause() != null) cause = cause.getCause();
                    error = cause.getMessage() != null && cause.getMessage().startsWith("api_") ? cause.getMessage() : "model_list_failed";
                } else models = result;
                if (minecraft.gui.screen() == ModelsScreen.this) rebuildWidgets();
            }));
        }
    }

    /*
     * Başlığı ve yükleniyor, boş ya da hata yazısını çizer.
     */
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        g.fill(0, 0, width, height, 0x8012151A); g.text(font, title, 10, 12, 0xFFF0F3F7);
        if (loading || !error.isEmpty() || models.isEmpty()) g.text(font, SettingsScreen.label(loading ? "loading_models" : error.isEmpty() ? "no_models" : error), 64, height - 44, 0xFFA2ABB5);
        super.extractRenderState(g, mx, my, dt);
    }

    /*
     * Bir önceki ekrana geri döner.
     */
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
