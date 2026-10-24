package org.hearlov.remscompanion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.hearlov.remscompanion.compat.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.Consumer;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
/** Small native multiline editor shared by all ports, including Minecraft 1.18. */
final class MultiLineEditBox extends AbstractWidget {
    private final Font font;
    private String value = "";
    private String wrappedValue;
    private int wrappedWidth;
    private List<Line> wrappedLines;
    private int cursor, anchor, limit = 4096, firstLine;
    private Consumer<String> listener = s -> {};
    private final boolean background;
    private final Component placeholder;
    private record Line(int start, int end) {}
    /*
     * kutuyu builder'dan gelen ayarlarla kuruyorum
     */
    private MultiLineEditBox(Font font, int x, int y, int width, int height, Component label, boolean background, Component placeholder) {
        super(x, y, width, height, label); this.font = font; this.background = background; this.placeholder = placeholder;
    }

    /*
     * vanilla'daki gibi builder ile açılsın diye
     */
    static Builder builder() { return new Builder(); }
    static final class Builder {
        int x, y; boolean background = true; Component placeholder = Component.empty();
        /*
         * x konumu
         */
        Builder setX(int value) { x = value; return this; }
        /*
         * y konumu
         */
        Builder setY(int value) { y = value; return this; }
        /*
         * arka plan çizilsin mi
         */
        Builder setShowBackground(boolean value) { background = value; return this; }
        /*
         * boşken görünen soluk yazı
         */
        Builder setPlaceholder(Component value) { placeholder = value; return this; }
        /*
         * toplanan ayarlarla kutuyu oluşturuyor
         */
        MultiLineEditBox build(Font font, int width, int height, Component label) { return new MultiLineEditBox(font, x, y, width, height, label, background, placeholder); }
    }

    /*
     * en fazla kaç karakter yazılabilir
     */
    void setCharacterLimit(int value) { limit = value; }
    /*
     * kutudaki metni veriyor
     */
    String getValue() { return value; }
    /*
     * metni limite göre kırpıp koyuyorum, değiştiyse
     * imleci sona alıp dinleyiciye haber veriyorum
     */
    void setValue(String text) {
        String safe = text.length() > limit ? text.substring(0, limit) : text;
        if (safe.equals(value)) return;
        value = safe; cursor = value.length(); anchor = cursor; reveal(); listener.accept(value);
    }

    /*
     * metin değişince çağrılacak fonksiyonu tutuyor
     */
    void setValueListener(Consumer<String> callback) { listener = callback; }
    /*
     * metni kutu genişliğine göre satırlara bölüyorum,
     * metin ve genişlik aynıysa önceki sonucu dönüyorum
     */
    private List<Line> lines() {
        if (wrappedValue == value && wrappedWidth == getWidth()) return wrappedLines;
        List<Line> result = new ArrayList<>(); int start = 0, available = Math.max(8, getWidth() - 12);
        while (start < value.length()) {
            int newline = value.indexOf('\n', start); int end = newline < 0 ? value.length() : newline;
            if (start == end) { result.add(new Line(start, end)); start++; continue; }
            while (start < end) {
                int count = font.plainSubstrByWidth(value.substring(start, end), available).length();
                if (count == 0) count = Character.charCount(value.codePointAt(start));
                result.add(new Line(start, start + count)); start += count;
            }
            if (newline >= 0) start++;
        }
        if (result.isEmpty() || value.endsWith("\n")) result.add(new Line(value.length(), value.length()));
        wrappedValue = value; wrappedWidth = getWidth(); wrappedLines = result; return result;
    }

    /*
     * imlecin hangi satırda olduğunu buluyor
     */
    private int cursorLine(List<Line> lines) {
        for (int i = 0; i < lines.size(); i++) if (cursor <= lines.get(i).end) return i;
        return lines.size() - 1;
    }

    /*
     * imleç hep görünsün diye ilk görünen satırı kaydırıyorum
     */
    private void reveal() {
        var lines = lines(); int row = cursorLine(lines), visible = Math.max(1, (getHeight() - 8) / 12);
        if (row < firstLine) firstLine = row;
        if (row >= firstLine + visible) firstLine = row - visible + 1;
        firstLine = Math.max(0, Math.min(firstLine, Math.max(0, lines.size() - visible)));
    }

    /*
     * imleci sınırlar içinde taşıyorum, shift yoksa seçim kalkıyor
     */
    private void move(int target, boolean shift) { cursor = Math.max(0, Math.min(value.length(), target)); if (!shift) anchor = cursor; reveal(); }
    /*
     * seçili kısmı verilen metinle değiştiriyorum, limiti aşan kısmı kesiyorum
     */
    private void replace(String text) {
        int a = Math.min(cursor, anchor), b = Math.max(cursor, anchor);
        int room = limit - (value.length() - (b - a)); if (text.length() > room) text = text.substring(0, Math.max(0, room));
        String updated = value.substring(0, a) + text + value.substring(b);
        cursor = a + text.length(); anchor = cursor;
        if (!updated.equals(value)) { value = updated; reveal(); listener.accept(value); }
    }

    /*
     * tuşların hepsi burada: ctrl+a/c/x/v, enter, silme,
     * oklar, home/end. odak yoksa hiçbirine bakmıyorum
     */
    private boolean key(int key, int mods) {
        if (!isFocused() || !active) return false;
        boolean shift = (mods & GLFW.GLFW_MOD_SHIFT) != 0, ctrl = (mods & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;
        var clipboard = Minecraft.getInstance().keyboardHandler;
        if (ctrl && key == GLFW.GLFW_KEY_A) { anchor = 0; cursor = value.length(); reveal(); return true; }
        if (ctrl && (key == GLFW.GLFW_KEY_C || key == GLFW.GLFW_KEY_X)) {
            clipboard.setClipboard(value.substring(Math.min(cursor, anchor), Math.max(cursor, anchor)));
            if (key == GLFW.GLFW_KEY_X) replace(""); return true;
        }
        if (ctrl && key == GLFW.GLFW_KEY_V) { replace(clipboard.getClipboard().replace("\r\n", "\n").replace("\r", "\n")); return true; }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { replace("\n"); return true; }
        if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
            if (cursor == anchor) {
                if (key == GLFW.GLFW_KEY_BACKSPACE && cursor > 0) anchor = value.offsetByCodePoints(cursor, -1);
                if (key == GLFW.GLFW_KEY_DELETE && cursor < value.length()) anchor = value.offsetByCodePoints(cursor, 1);
            }
            replace(""); return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
            int direction = key == GLFW.GLFW_KEY_LEFT ? -1 : 1;
            int target = cursor;
            if (direction < 0 && cursor > 0 || direction > 0 && cursor < value.length()) target = value.offsetByCodePoints(cursor, direction);
            move(target, shift); return true;
        }
        var lines = lines(); int row = cursorLine(lines); Line line = lines.get(row);
        if (key == GLFW.GLFW_KEY_HOME || key == GLFW.GLFW_KEY_END) { move(ctrl ? (key == GLFW.GLFW_KEY_HOME ? 0 : value.length()) : (key == GLFW.GLFW_KEY_HOME ? line.start : line.end), shift); return true; }
        if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
            int x = font.width(value.substring(line.start, Math.min(cursor, line.end)));
            Line next = lines.get(Math.max(0, Math.min(lines.size() - 1, row + (key == GLFW.GLFW_KEY_UP ? -1 : 1))));
            move(next.start + font.plainSubstrByWidth(value.substring(next.start, next.end), x).length(), shift); return true;
        }
        return false;
    }

    /*
     * sol tıkla kutuya odaklanıp imleci tıklanan yere koyuyorum
     */
    private boolean click(double mx, double my, int button) {
        if (button != 0 || !active || !isMouseOver(mx, my)) return false;
        setFocused(true); point(mx, my, false); return true;
    }

    /*
     * fare konumunu metindeki yere çeviriyorum, select açıksa seçimi uzatıyor
     */
    private void point(double mx, double my, boolean select) {
        var lines = lines(); int row = Math.max(0, Math.min(lines.size() - 1, firstLine + (int)(my - @Y@ - 4) / 12));
        Line line = lines.get(row); move(line.start + font.plainSubstrByWidth(value.substring(line.start, line.end), Math.max(0, (int)mx - @X@ - 5)).length(), select);
    }
    /*LEGACY_INPUT*/
    /*
     * eski tuş imzası, ortak key metoduna gidiyor
     */
    @Override public boolean keyPressed(int key, int scan, int mods) { return key(key, mods); }
    /*
     * yazılan karakteri ekliyorum, kontrol karakterlerini almıyorum
     */
    @Override public boolean charTyped(char ch, int mods) { if (!isFocused() || !active || Character.isISOControl(ch)) return false; replace(String.valueOf(ch)); return true; }
    /*
     * eski tıklama imzası
     */
    @Override public boolean mouseClicked(double x, double y, int button) { return click(x, y, button); }
    /*
     * eski sürükleme imzası, seçimi uzatıyor
     */
    @Override protected void onDrag(double x, double y, double dx, double dy) { point(x, y, true); }
    /*ENDLEGACY_INPUT*/
    /*NEW_INPUT*/
    /*
     * yeni KeyEvent ile tuş, yine ortak key metodu
     */
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent e) { return key(e.key(), e.modifiers()); }
    /*
     * yeni karakter olayı, geçersiz kod noktası gelirse almıyorum
     */
    @Override public boolean charTyped(net.minecraft.client.input.CharacterEvent e) { if (!isFocused() || !active || !Character.isValidCodePoint(e.codepoint())) return false; replace(new String(Character.toChars(e.codepoint()))); return true; }
    /*
     * yeni tıklama olayı
     */
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent e, boolean twice) { return click(e.x(), e.y(), e.button()); }
    /*
     * yeni sürükleme olayı, seçimi uzatıyor
     */
    @Override protected void onDrag(net.minecraft.client.input.MouseButtonEvent e, double dx, double dy) { point(e.x(), e.y(), true); }
    /*ENDNEW_INPUT*/
    /*
     * fare tekerine göre ilk satırı kaydırıyorum, sınır dışına çıkmasın
     */
    private boolean scroll(double amount) { firstLine = Math.max(0, Math.min(Math.max(0, lines().size() - Math.max(1, (getHeight() - 8) / 12)), firstLine - (int)(amount * 3))); return true; }
    /*
     * 1.21 öncesi kaydırma, tek değer geliyor
     */
    /*PRE21*/@Override public boolean mouseScrolled(double x, double y, double amount) { return scroll(amount); }/*ENDPRE21*/
    /*
     * 1.21 ve sonrası, sadece dikey değeri kullanıyorum
     */
    /*POST21*/@Override public boolean mouseScrolled(double x, double y, double h, double v) { return scroll(v); }/*ENDPOST21*/
    /*OLD*/
    /*
     * eski sürümde odak, direkt üst sınıfa
     */
    @Override public void setFocused(boolean value) { super.setFocused(value); }
    /*
     * eski sürümün anlatıcı metni, buton gibi okunuyor
     */
    @Override public void updateNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
    /*ENDOLD*/
    /*
     * yeni sürümlerde anlatıcı metni, yine buton gibi
     */
    /*MODERN*/@Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }/*ENDMODERN*/
    /*
     * kutuyu çiziyorum: zemin, kırpılmış satırlar, placeholder,
     * seçim ve yanıp sönen imleç. metot adı sürüme göre değişiyor
     */
    @Override public void @WIDGET_RENDER@(@NATIVE_GRAPHICS@ nativeGraphics, int mx, int my, float dt) {
        var g = new GuiGraphicsExtractor(nativeGraphics); int bx = @X@, by = @Y@;
        if (background) g.fill(bx, by, bx + getWidth(), by + getHeight(), 0x7020242B);
        g.enableScissor(bx, by, bx + getWidth(), by + getHeight());
        var lines = lines(); int visible = Math.max(1, (getHeight() - 8) / 12), cursorRow = cursorLine(lines);
        if (value.isEmpty() && !isFocused()) g.text(font, placeholder, bx + 5, by + 4, 0xFF808A97);
        for (int row = firstLine; row < Math.min(lines.size(), firstLine + visible); row++) {
            Line line = lines.get(row); int y = by + 4 + (row - firstLine) * 12;
            int a = Math.max(line.start, Math.min(cursor, anchor)), b = Math.min(line.end, Math.max(cursor, anchor));
            if (a < b) g.fill(bx + 5 + font.width(value.substring(line.start, a)), y, bx + 5 + font.width(value.substring(line.start, b)), y + 10, 0x805A88AD);
            g.text(font, value.substring(line.start, line.end), bx + 5, y, 0xFFE0E5EB);
            if (isFocused() && row == cursorRow && System.currentTimeMillis() / 500 % 2 == 0) {
                int cx = bx + 5 + font.width(value.substring(line.start, Math.min(cursor, line.end))); g.fill(cx, y, cx + 1, y + 10, 0xFFE0E5EB);
            }
        }
        g.disableScissor();
    }
}
