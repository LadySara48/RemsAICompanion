package org.hearlov.remscompanion;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.List;
import org.hearlov.remscompanion.api.*;
import org.hearlov.remscompanion.storage.ContextWindow;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
public final class CompanionScreen extends MinimalScreen {
    private MultiLineEditBox input;
    private Button send, regenerate;
    private net.minecraft.client.gui.components.EditBox search;
    private String query = "";
    private boolean draggingScroll; private double scrollGrab;
    private int left, right, top, bottom, sidebarWidth, portraitWidth;
    private int chatX, chatRight, scroll, sidebarOffset, areaTop, areaBottom, composerTop, portraitSize;
    private boolean syncingInput;
    private final ChatContent content = new ChatContent();

    /*
     * Ana ekranı açar: sohbetleri yükler, hiç yoksa yenisini açar
     * ve listeyi seçili sohbete kaydırır.
     */
    public CompanionScreen() {
        super(text("title"));
        ChatSession.load();
        if (ChatSession.selected == null) ChatSession.create();
        sidebarOffset = Math.max(0, ChatSession.SESSIONS.indexOf(ChatSession.selected));
    }

    /*
     * Çeviri anahtarına mod önekini ekleyip ekran yazısı yapar.
     */
    private static Component text(String key) {
        return Component.translatable("screen.rems_ai_companion." + key);
    }

    /*
     * İpucu yazılı küçük bir buton ekleyip geri döner.
     */
    private Button button(Component label, int x, int y, int w, Runnable action) {
        Button button = this.addRenderableWidget(new MinimalButton(x, y, w, 20, label, action));
        button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(label)); return button;
    }

    /*
     * Bütün arayüzü kurar: kenar çubuğu, arama, sohbet listesi,
     * yazı kutusu, gönder ve yenile butonları.
     */
    @Override protected void init() {
        chatButtons.clear();
        left = 6; right = width - 6; top = 6; bottom = height - 6;
        sidebarWidth = width >= 520 ? 120 : 72;
        portraitWidth = width >= 520 ? 124 : 86;
        chatX = left + sidebarWidth + 8;
        chatRight = right - portraitWidth - 8;
        portraitSize = Math.min(portraitWidth - 12, Math.max(32, height - 188));
        int actionsY = top + 32 + portraitSize + 8;
        int actionX = right - portraitWidth + 2;
        button(text("prompts"), actionX, actionsY, portraitWidth - 4, () -> minecraft.gui.setScreen(new PromptScreen(this)));
        button(text("general_settings"), actionX, bottom - 48, portraitWidth - 4, () -> minecraft.gui.setScreen(new SettingsScreen(this)));
        button(text("settings"), actionX, actionsY + 24, portraitWidth - 4, () -> minecraft.gui.setScreen(new ConnectionScreen(this)));
        button(text("close"), actionX, bottom - 24, portraitWidth - 4, this::onClose);
        button(text("new_chat"), left + 4, top + 35, sidebarWidth - 8, () -> {
            query = ""; ChatSession.create(); sidebarOffset = Math.max(0, ChatSession.SESSIONS.indexOf(ChatSession.selected) - visibleChats() + 1);
            scroll = 0; rebuildWidgets();
        });
        search = new net.minecraft.client.gui.components.EditBox(font, left + 4, top + 61, sidebarWidth - 8, 18, text("search_chats"));
        search.setMaxLength(100); search.setHint(text("search_chats")); search.setValue(query);
        search.setResponder(value -> { query = value; sidebarOffset = 0; rebuildWidgets(); setFocused(search); search.setCursorPosition(value.length()); });
        addRenderableWidget(search);
        sidebarOffset = Math.min(sidebarOffset, Math.max(0, filteredChats().size() - visibleChats()));
        for (int i = 0; i < visibleChats() && i + sidebarOffset < filteredChats().size(); i++) {
            ChatSession session = filteredChats().get(i + sidebarOffset);
            Button entry = button(chatLabel(session), left + 4, top + 88 + i * 24, sidebarWidth - 8, () -> {
                ChatSession.select(session); scroll = 0; rebuildWidgets();
            });
            entry.active = session != ChatSession.selected;
            ((MinimalButton) entry).selected = session == ChatSession.selected;
            entry.setTooltip(net.minecraft.client.gui.components.Tooltip.create(chatLabel(session)));
            chatButtons.put(session, entry);
        }
        button(text("chat_settings"), left + 2, bottom - 50, sidebarWidth - 4,
                () -> minecraft.gui.setScreen(new ChatSettingsScreen(this, ChatSession.selected)));
        Button prev = button(Component.literal("<"), left + 4, bottom - 26, 26, () -> {
            sidebarOffset = Math.max(0, sidebarOffset - visibleChats()); rebuildWidgets();
        });
        prev.active = sidebarOffset > 0;
        Button next = button(Component.literal(">"), left + sidebarWidth - 30, bottom - 26, 26, () -> {
            sidebarOffset += visibleChats(); rebuildWidgets();
        });
        next.active = sidebarOffset + visibleChats() < filteredChats().size();
        composerTop = bottom - 62; areaTop = top + 60; areaBottom = composerTop - 10;
        input = MultiLineEditBox.builder().setX(chatX).setY(composerTop + 4).setShowBackground(false)
                .setPlaceholder(text("input")).build(font, Math.max(24, chatRight - chatX - 76), 50, text("input"));
        input.setCharacterLimit(4096); input.setValue(ChatSession.selected.draft);
        input.setValueListener(value -> {
            if (syncingInput) return;
            if (!value.equals(ChatSession.selected.draft)) CompanionVoices.input(value.length() > ChatSession.selected.draft.length());
            ChatSession.selected.draft = value; ChatSession.selected.changed();
        });
        input.setTooltip(net.minecraft.client.gui.components.Tooltip.create(text("input_help")));
        addRenderableWidget(input);
        send = button(text("send"), chatRight - 48, bottom - 29, 48, this::submit);
        regenerate = button(Component.literal("↻"), chatRight - 72, bottom - 29, 20, this::regenerate);
        regenerate.setTooltip(net.minecraft.client.gui.components.Tooltip.create(text("regenerate")));
        setInitialFocus(input);
    }
    private final java.util.Map<ChatSession, Button> chatButtons = new java.util.HashMap<>();
    /*
     * Sohbetin listede görünecek adını verir, sabitlenmişse başına yıldız koyar.
     */
    private Component chatLabel(ChatSession session) {
        Component label = session.title.isBlank() ? Component.translatable("screen.rems_ai_companion.chat_number", session.number) : Component.literal(session.title);
        return session.pinned ? Component.literal("★ ").append(label) : label;
    }

    /*
     * Ekran yüksekliğine göre listeye kaç sohbet sığdığını hesaplar.
     */
    private int visibleChats() { return Math.max(1, (height - 160) / 24); }
    /*
     * Arama kutusuna göre başlıkta ya da mesajlarda geçen sohbetleri süzer.
     */
    private List<ChatSession> filteredChats() {
        if (query.isBlank()) return ChatSession.SESSIONS;
        String term = query.toLowerCase(java.util.Locale.ROOT);
        return ChatSession.SESSIONS.stream().filter(s -> s.title.toLowerCase(java.util.Locale.ROOT).contains(term)
                || s.messages.stream().anyMatch(m -> m.text.toLowerCase(java.util.Locale.ROOT).contains(term))).toList();
    }

    /*
     * Kaydırma tutamacının boyunu içerik uzunluğuna göre hesaplar.
     */
    private int thumbHeight() {
        int h = Math.max(1, areaBottom - areaTop); return Math.max(18, h * h / Math.max(h, content.totalHeight()));
    }

    /*
     * Tutamacın şu anki scroll'a göre nerede duracağını bulur.
     */
    private int thumbTop() {
        return areaTop + (content.maxScroll() == 0 ? 0 : (int) ((long) (content.maxScroll() - scroll) * (areaBottom - areaTop - thumbHeight()) / content.maxScroll()));
    }

    /*
     * Kaydırılacak içerik varsa scrollbar'ı çizer.
     */
    private void drawScrollbar(GuiGraphicsExtractor g) {
        if (content.maxScroll() <= 0) return;
        g.fill(chatRight - 4, areaTop, chatRight - 1, areaBottom, 0x15FFFFFF);
        int y = thumbTop(); g.fill(chatRight - 5, y, chatRight, y + thumbHeight(), 0x90ABBCC8);
    }

    /*
     * Tutamaç sürüklenirken fare konumunu scroll değerine çevirir.
     */
    private void dragScroll(double mouseY) {
        int range = areaBottom - areaTop - thumbHeight();
        if (range > 0) scroll = content.maxScroll() - (int) (Math.clamp((mouseY - scrollGrab - areaTop) / range, 0.0, 1.0) * content.maxScroll());
    }

    /*
     * Yazı kutusunu sohbetin taslağıyla eşitler.
     */
    private void syncInput() {
        if (input != null && !input.getValue().equals(ChatSession.selected.draft)) {
            syncingInput = true;
            try { input.setValue(ChatSession.selected.draft); }
            finally { syncingInput = false; }
        }
    }

    /*
     * Scrollbar, sürüm okları, kopyala ve link tıklamalarını yakalar,
     * gerisini Minecraft'a bırakır.
     */
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && content.maxScroll() > 0 && event.x() >= chatRight - 7
                && event.x() <= chatRight + 2 && event.y() >= areaTop && event.y() < areaBottom) {
            draggingScroll = true; scrollGrab = event.y() >= thumbTop() && event.y() < thumbTop() + thumbHeight()
                    ? event.y() - thumbTop() : thumbHeight() / 2.0; dragScroll(event.y()); return true;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && event.x() >= chatX && event.x() <= chatRight && event.y() >= areaTop && event.y() < areaBottom) {
            if (content.versionAt(event.x(), event.y(), ChatSession.selected)) return true;
            if (content.copyAt(event.x(), event.y(), minecraft.keyboardHandler::setClipboard)) return true;
            if (content.link() != null) { defaultHandleClickEvent(content.link(), minecraft, this); return true; }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /*
     * Scrollbar tutuluyorsa sürüklemeyle kaydırır.
     */
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        if (draggingScroll) { dragScroll(event.y()); return true; }
        return super.mouseDragged(event, dx, dy);
    }

    /*
     * Fare bırakılınca scrollbar sürüklemesini bitirir.
     */
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (draggingScroll) { draggingScroll = false; return true; } return super.mouseReleased(event);
    }

    /*
     * Her tick'te yazı kutusunu eşitler, yenile butonunu açıp kapatır
     * ve değişen sohbet adlarını günceller.
     */
    @Override public void tick() {
        super.tick();
        syncInput();
        var current = ChatSession.selected;
        regenerate.active = !current.busy && current.messages.size() >= 2 && current.messages.getLast().complete
                && current.messages.getLast().regenerations < 3 && current.messages.size() - 2 >= current.summaryThrough;
        for (var entry : chatButtons.entrySet()) {
            Component label = chatLabel(entry.getKey());
            if (!entry.getValue().getMessage().getString().equals(label.getString())) {
                entry.getValue().setMessage(label);
                entry.getValue().setTooltip(net.minecraft.client.gui.components.Tooltip.create(label));
            }
        }
    }

    /*
     * Yazılanı gönderir, başarılıysa kutuyu temizleyip en alta kaydırır.
     */
    private void submit() {
        if (ChatRequests.send(ChatSession.selected, input.getValue(), false)) { syncInput(); scroll = 0; setFocused(input); }
    }

    /*
     * Son kullanıcı mesajı için cevabı yeniden ürettirir.
     */
    private void regenerate() {
        var session = ChatSession.selected;
        if (session.messages.size() >= 2 && ChatRequests.send(session, session.messages.get(session.messages.size() - 2).text, true)) scroll = 0;
    }

    /*
     * Shift'siz Enter'a basınca mesajı gönderir,
     * cevap beklenirken Rem'in sızlanma sesini tetikler.
     */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (input.isFocused() && !event.hasShiftDown() && (event.key() == InputConstants.KEY_RETURN
                || event.key() == InputConstants.KEY_NUMPADENTER)) {
            if (ChatSession.selected.busy) { if (!input.getValue().isBlank()) CompanionVoices.BEHAVIOUR.blockedSend(); return true; }
            submit(); return true;
        }
        return super.keyPressed(event);
    }

    /*
     * Fare sohbet alanındaysa tekerlekle mesajları kaydırır.
     */
    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= chatX && x <= chatRight && y >= areaTop && y < areaBottom) {
            scroll = Math.max(0, scroll + (int) (vertical * 24));
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    /*
     * Ana ekranı çizer: paneller, durum yazısı, sohbet içeriği,
     * scrollbar ve Rem'in portresi.
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(left, top, right, bottom, 0x8012151A);
        g.fill(left + 8, top + 29, right - 8, top + 30, 0x16FFFFFF);
        g.fill(left + sidebarWidth + 2, top + 40, left + sidebarWidth + 3, bottom - 70, 0x12FFFFFF);
        g.fill(chatX - 3, composerTop, chatRight + 2, bottom - 6, 0x10FFFFFF);
        g.fill(chatX - 3, bottom - 7, chatRight + 2, bottom - 6, 0x308FCFE8);
        g.text(font, title, left + 8, top + 12, 0xFFF0F3F7);
        int noticeY = top + 36;
        ChatSession session = ChatSession.selected;
        send.setMessage(text(session.busy ? "stop" : "send"));
        send.active = session.busy || (!session.atLimit() && !input.getValue().isBlank());
        Component status = !session.error.isEmpty()
                ? (session.error.startsWith("HTTP ") ? Component.translatable("screen.rems_ai_companion.http_error", session.error.substring(5)) : text(session.error))
                : ChatSession.saveFailed ? text("chat_save_failed") : ChatSession.unreadableChats ? text("chat_load_warning")
                : session.busy ? text("responding") : session.atLimit() ? text("chat_limit")
                : Component.translatable("screen.rems_ai_companion.chat_status", session.profile().name, session.userMessages());
        for (FormattedCharSequence line : font.split(status, Math.max(20, chatRight - chatX - 4))) {
            if (noticeY >= top + 57) break;
            g.text(font, line, chatX, noticeY, 0xFF9AAEC7); noticeY += 10;
        }
        scroll = content.render(g, font, session, chatX, chatRight - 9, areaTop, areaBottom, scroll, mouseX, mouseY);
        drawScrollbar(g);
        if (portraitWidth > 0) {
            int x = right - portraitWidth;
            boolean streaming = session.busy && !session.messages.isEmpty() && !session.messages.getLast().text.isEmpty();
            String image = CompanionVoices.BEHAVIOUR.image(session.busy, streaming, !session.draft.isEmpty());
            Identifier texture = Identifier.fromNamespaceAndPath(RemsAICompanionClient.MOD_ID, "textures/gui/rem/" + image + ".png");
            int size = portraitSize;
            g.blit(texture, x + 3, top + 32, x + 3 + size, top + 32 + size, 0, 1, 0, 1);
            g.text(font, Component.literal("Rem"), x + 10, top + 16, 0xFFD4D9DF);
        }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    /*
     * Bu ekran açıkken oyun durmasın diye false döner.
     */
    @Override
    public boolean isPauseScreen() { return false; }

    /*
     * Ekran kapanınca kaydedilmemişleri yazar ve çalan sesi durdurur.
     */
    @Override
    public void removed() { ChatSession.flush(); CompanionVoices.stop(minecraft); super.removed(); }
}





