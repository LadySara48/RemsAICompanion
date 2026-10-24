package org.hearlov.remscompanion;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.time.LocalDate;
import java.util.Locale;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
final class StatisticsScreen extends MinimalScreen {
    private final Screen parent;
    /*
     * İstatistik ekranını kurar, geri dönülecek ekranı saklar.
     */
    StatisticsScreen(Screen parent) { super(SettingsScreen.label("statistics")); this.parent = parent; }
    /*
     * Sadece geri butonunu ekler.
     */
    @Override protected void init() {
        int w = Math.min(560, width - 20), x = (width - w) / 2;
        addRenderableWidget(new MinimalButton(x, height - 26, w, 20, SettingsScreen.label("back"), this::onClose));
    }

    /*
     * Toplam ve son günlerin token ve maliyet satırlarını hesaplayıp çizer.
     * Bilinmeyenler n/a, eksik olanlar yıldızla gösterilir.
     */
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        int w = Math.min(560, width - 20), x = (width - w) / 2;
        g.fill(0, 0, width, height, 0x8012151A); g.text(font, title, x, 12, 0xFFF0F3F7);
        String[] names = {"usage_all", "usage_today", "usage_yesterday", "usage_2days", "usage_3days", "usage_week", "usage_2weeks", "usage_3weeks", "usage_month"};
        int[] spans = {0, 1, 2, 3, 4, 7, 14, 21, 30};
        LocalDate today = LocalDate.now(); AppState.ledger.prune(today);
        for (int i = 0; i < names.length; i++) {
            var total = i == 0 ? AppState.ledger.lifetime : AppState.ledger.since(today.minusDays(spans[i] - 1), today);
            String tokens = total.requests > 0 && total.unreported == total.requests ? "tokens n/a" : total.tokens + " tokens" + (total.unreported > 0 ? "*" : "");
            String cost = total.costReports == 0 ? "USD n/a" : "$" + String.format(Locale.ROOT, "%.6f", total.usd) + (total.costReports < total.requests ? "*" : "");
            Component row = Component.translatable("screen.rems_ai_companion." + names[i]).append(": " + tokens + " | " + cost);
            g.text(font, font.plainSubstrByWidth(row.getString(), w), x, 34 + i * 17, i == 0 ? 0xFFFF8585 : 0xFFD4D9DF);
        }
        int y = 192;
        for (var line : font.split(SettingsScreen.label(AppState.usageFailed ? "usage_unavailable" : "usage_note"), w)) {
            if (y >= height - 30) break; g.text(font, line, x, y, 0xFFA2ABB5); y += 10;
        }
        super.extractRenderState(g, mx, my, dt);
    }

    /*
     * Bir önceki ekrana geri döner.
     */
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
