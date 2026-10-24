package org.hearlov.remscompanion.chat;

import java.util.*;
import java.util.function.Consumer;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
// çizerken de tıklarken de aynı kutuları kullanıyorum, sohbet alanına kırpılmış halde
public final class CopyTargets {
    private record Hit(int x, int y, int width, int height, String text) {}
    private final List<Hit> hits = new ArrayList<>();
    private int left, right, top, bottom;

    public void reset(int left, int right, int top, int bottom) {
        hits.clear(); this.left = left; this.right = right; this.top = top; this.bottom = bottom;
    }
    public void add(int x, int y, int width, int height, String text) { hits.add(new Hit(x, y, width, height, text)); }

    public boolean click(double x, double y, Consumer<String> clipboard) {
        if (x < left || x >= right || y < top || y >= bottom) return false; // alanın dışıysa hiç bakma

        for (Hit hit : hits) {
            if (x >= hit.x && x < hit.x + hit.width && y >= hit.y && y < hit.y + hit.height) {
                clipboard.accept(hit.text); return true;
            }
        }

        return false;
    }
}
