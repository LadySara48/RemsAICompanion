package org.hearlov.remscompanion;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;
import org.hearlov.remscompanion.chat.Markdown;
import java.net.URI;
import java.util.*;
import java.util.regex.*;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
/** Text layout, safe markdown styling and clipped copy/link hit areas. */
final class ChatContent {
    private record Piece(List<FormattedCharSequence> lines, int width, int height, boolean user, boolean code,
                         String language, String copy, boolean footer, int color, ChatSession.Message message) {}
    private record VersionHit(int x, int y, ChatSession.Message message, int delta) {}
    private final List<VersionHit> versionHits = new ArrayList<>();
    private int totalHeight, maxScroll, previousHeight, previousWidth;
    private ChatSession previousSession;
    /*
     * Sohbet içeriğinin toplam yüksekliğini verir, kaydırma için lazım.
     */
    int totalHeight() { return totalHeight; }
    /*
     * En fazla ne kadar kaydırılabileceğini döner.
     */
    int maxScroll() { return maxScroll; }
    private record Hit(int x, int y, int width, int height, String value) {
        /*
         * Fare bu kopyala alanının içinde mi, ona bakar.
         */
        boolean contains(double mx, double my) { return mx >= x && mx < x + width && my >= y && my < y + height; }
    }
    private static final Pattern INLINE = Pattern.compile("\\*\\*(.+?)\\*\\*|__(.+?)__|`([^`]+)`|\\*([^*]+)\\*|_([^_]+)_|\\[([^\\]]+)\\]\\((https?://[^\\s)]+)\\)");
    private final org.hearlov.remscompanion.chat.CopyTargets copies = new org.hearlov.remscompanion.chat.CopyTargets();
    private ClickEvent hoveredLink;
    private String copied = "";
    private long copiedUntil;
    private int viewTop, viewBottom, viewLeft, viewRight;
    private List<Piece> cache = List.of();
    private ChatSession cachedSession;
    private int cachedWidth, cachedCount, cachedSources;
    private String cachedLast;
    private boolean cachedBusy;

    /*
     * Satır içi markdown'ı (kalın, italik, `kod`, link) Minecraft stillerine çevirir.
     * Eşleşmeyen düz metni de olduğu gibi ekler.
     */
    static Component rich(String input, Style base) {
        var result = Component.empty(); Matcher matcher = INLINE.matcher(input); int start = 0;
        while (matcher.find()) {
            result.append(Component.literal(input.substring(start, matcher.start())).withStyle(base));
            Style style = base; String text;
            if (matcher.group(1) != null || matcher.group(2) != null) {
                text = matcher.group(1) != null ? matcher.group(1) : matcher.group(2); style = style.withBold(true);
            } else if (matcher.group(3) != null) { text = matcher.group(3); style = style.withColor(0xE8BFD2); }
            else if (matcher.group(4) != null || matcher.group(5) != null) {
                text = matcher.group(4) != null ? matcher.group(4) : matcher.group(5); style = style.withItalic(true);
            } else {
                text = matcher.group(6);
                try { style = style.withColor(0xA7D7EB).withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(URI.create(matcher.group(7)))); }
                catch (IllegalArgumentException ignored) {}
            }
            result.append(Component.literal(text).withStyle(style)); start = matcher.end();
        }
        result.append(Component.literal(input.substring(start)).withStyle(base)); return result;
    }

    /*
     * Metni verilen genişliğe göre satırlara böler,
     * balonun boyutunu hesaplayıp tek bir parça olarak döner.
     */
    private Piece piece(Font font, Component text, int maxWidth, boolean user, boolean code, String language, String copy, int color) {
        List<FormattedCharSequence> lines = font.split(text, Math.max(8, maxWidth - (user || code ? 16 : 0)));
        int measured = lines.stream().mapToInt(font::width).max().orElse(8);
        int w = code ? maxWidth : Math.min(maxWidth, Math.max(24, measured + (user ? 16 : 0)));
        return new Piece(lines, w, lines.size() * 12 + (code ? 24 : user ? 10 : 4), user, code, language, copy, false, color, null);
    }

    /*
     * Oturumdaki mesajları ekrana çizilecek parçalara dönüştürür.
     * Hiçbir şey değişmediyse önbellekteki listeyi verir, boşuna hesap yapmaz.
     */
    private List<Piece> layout(Font font, ChatSession session, int width) {
        String last = session.messages.isEmpty() ? "" : session.messages.getLast().text;
        int sources = session.sources.hashCode();
        if (session == cachedSession && width == cachedWidth && session.messages.size() == cachedCount
                && last == cachedLast && session.busy == cachedBusy && sources == cachedSources) return cache;
        List<Piece> pieces = new ArrayList<>();
        if (session.messages.isEmpty()) pieces.add(piece(font, label("welcome"), width, false, false, "", "", 0xFFA2ABB5));
        for (ChatSession.Message message : session.messages) {
            boolean user = message.role.equals("user");
            if (user) {
                pieces.add(piece(font, Component.literal(message.text), Math.max(24, width * 78 / 100), true, false, "", "", 0xFFFFEAF2));
            } else {
                for (Markdown.Block block : Markdown.parse(message.text)) {
                    boolean code = block.kind() == Markdown.Kind.CODE;
                    Style style = Style.EMPTY;
                    if (block.kind() == Markdown.Kind.HEADING) style = style.withBold(true);
                    if (block.kind() == Markdown.Kind.QUOTE) style = style.withItalic(true).withColor(0xA2ABB5);
                    pieces.add(piece(font, code ? Component.literal(block.text().replace("\t", "    ")) : rich(block.text(), style),
                            width, false, code, block.language(), code ? block.text() : "", 0xFFE0E5EB));
                }
                if (!message.complete && !session.busy) pieces.add(piece(font, label("partial_response"), width, false, false, "", "", 0xFFA2ABB5));
            }
            if (!message.text.isEmpty()) pieces.add(new Piece(List.of(), width, 16, user, false, "", message.text, true, 0xFFA2ABB5, message));
        }
        for (String url : session.sources) pieces.add(piece(font, rich("[" + url + "](" + url + ")", Style.EMPTY), width, false, false, "", "", 0xFFA0BFD4));
        cachedSession = session; cachedWidth = width; cachedCount = session.messages.size(); cachedLast = last;
        cachedBusy = session.busy; cachedSources = sources; cache = pieces; return pieces;
    }

    /*
     * Sohbeti kırpılmış alanın içine çizer: balonlar, kod blokları, kopyala butonları, sürüm okları.
     * Yeni içerik gelince kaydırmayı korur ve son scroll değerini döner.
     */
    int render(GuiGraphicsExtractor g, Font font, ChatSession session, int left, int right, int top, int bottom, int scroll, int mx, int my) {
        copies.reset(left, right, top, bottom); versionHits.clear(); hoveredLink = null; viewTop = top; viewBottom = bottom; viewLeft = left; viewRight = right;
        List<Piece> pieces = layout(font, session, Math.max(24, right - left - 8));
        int height = pieces.stream().mapToInt(p -> p.height + (p.footer ? 10 : 5)).sum();
        int available = Math.max(0, bottom - top);
        if (scroll > 0 && previousSession == session && previousWidth == right - left) scroll += height - previousHeight;
        previousSession = session; previousWidth = right - left; previousHeight = height;
        totalHeight = height; maxScroll = Math.max(0, height - available); scroll = Math.clamp(scroll, 0, maxScroll);
        int y = top + Math.min(0, available - height) + scroll;
        g.enableScissor(left, top, right, bottom);
        for (Piece p : pieces) {
            if (y + p.height >= top && y < bottom) {
                int x = p.user ? right - 4 - p.width : left + 4;
                if (p.footer) {
                    drawCopy(g, font, p.user ? right - 42 : left + 4, y, p.copy, mx, my);
                    if (!p.user && p.message.alternatives.size() > 1 && !session.busy && session.messages.indexOf(p.message) >= session.summaryThrough) {
                        int vx = left + 50;
                        g.text(font, "‹", vx, y + 3, 0xFFA2ABB5);
                        g.text(font, (p.message.variant + 1) + "/" + p.message.alternatives.size(), vx + 14, y + 3, 0xFFA2ABB5);
                        g.text(font, "›", vx + 40, y + 3, 0xFFA2ABB5);
                        versionHits.add(new VersionHit(vx - 2, y, p.message, -1)); versionHits.add(new VersionHit(vx + 38, y, p.message, 1));
                    }
                } else {
                    if (p.user) {
                        g.fill(x + 3, y, x + p.width - 3, y + 3, 0x40E6A0BE);
                        g.fill(x, y + 3, x + p.width, y + p.height - 3, 0x40E6A0BE);
                        g.fill(x + 3, y + p.height - 3, x + p.width - 3, y + p.height, 0x40E6A0BE);
                    }
                    if (p.code) {
                        g.fill(x, y, x + p.width, y + p.height, 0x7010141B);
                        g.text(font, font.plainSubstrByWidth(p.language.isBlank() ? "code" : p.language, Math.max(8, p.width - 58)), x + 6, y + 4, 0xFFA2ABB5);
                        drawCopy(g, font, x + p.width - 42, y + 1, p.copy, mx, my);
                    }
                    int lineY = y + (p.code ? 20 : p.user ? 6 : 2);
                    for (FormattedCharSequence line : p.lines) {
                        int textX = x + (p.user || p.code ? 8 : 0);
                        g.text(font, line, textX, lineY, p.color);
                        if (my >= top && my < bottom && my >= lineY && my < lineY + 10 && mx >= textX && mx <= textX + font.width(line)) {
                            Style style = styleAt(font, line, mx - textX);
                            if (style != null && style.getClickEvent() instanceof ClickEvent.OpenUrl link) hoveredLink = link;
                        }
                        lineY += 12;
                    }
                }
            }
            y += p.height + (p.footer ? 10 : 5);
        }
        g.disableScissor(); return scroll;
    }

    /*
     * Çeviri anahtarına mod önekini ekleyip ekran yazısı yapar.
     */
    private static Component label(String key) { return Component.translatable("screen.rems_ai_companion." + key); }
    /*
     * Satırda verilen x pikseline denk gelen karakterin stilini bulur, link tıklaması için.
     */
    private static Style styleAt(Font font, FormattedCharSequence line, int target) {
        float[] width = {0}; Style[] result = {null};
        line.accept((index, style, codepoint) -> {
            width[0] += font.getSplitter().stringWidth(Component.literal(new String(Character.toChars(codepoint))).withStyle(style));
            if (width[0] > target) { result[0] = style; return false; }
            return true;
        });
        return result[0];
    }

    /*
     * Kopyala / kopyalandı yazısını çizer ve tıklanabilir alanını kaydeder.
     */
    private void drawCopy(GuiGraphicsExtractor g, Font font, int x, int y, String value, int mx, int my) {
        Hit hit = new Hit(x, y, 40, 14, value); copies.add(x, y, 40, 14, value);
        if (hit.contains(mx, my)) g.fill(x, y, x + 40, y + 14, 0x20FFFFFF);
        g.text(font, label(value.equals(copied) && System.nanoTime() < copiedUntil ? "copied" : "copy"), x + 2, y + 3, 0xFFA2ABB5, false);
    }

    /*
     * Tıklanan yer bir kopyala alanıysa metni panoya atar,
     * kısa bir süre 'kopyalandı' yazsın diye işaretler.
     */
    boolean copyAt(double x, double y, java.util.function.Consumer<String> clipboard) {
        return copies.click(x, y, value -> {
            clipboard.accept(value); copied = value; copiedUntil = System.nanoTime() + 1_200_000_000L;
        });
    }

    /*
     * Farenin şu an üstünde durduğu linki döner, yoksa null.
     */
    ClickEvent link() { return hoveredLink; }
    /*
     * Cevap sürüm oklarına tıklandıysa önceki ya da sonraki cevaba geçer ve sohbeti kaydeder.
     */
    boolean versionAt(double x, double y, ChatSession session) {
        if (session.busy || x < viewLeft || x >= viewRight || y < viewTop || y >= viewBottom) return false;
        for (var hit : versionHits) if (x >= hit.x && x < hit.x + 13 && y >= hit.y && y < hit.y + 14) {
            org.hearlov.remscompanion.chat.AnswerVersions.choose(hit.message, hit.delta);
            if (session.messages.getLast() == hit.message) { session.sources.clear(); session.sources.addAll(org.hearlov.remscompanion.chat.AnswerVersions.sources(hit.message)); }
            session.changed(); session.save(); cachedSession = null; return true;
        }
        return false;
    }
}
