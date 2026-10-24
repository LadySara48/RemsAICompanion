package org.hearlov.remscompanion.chat;

import java.util.*;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
// markdown'ın küçük, güvenli bi kısmı sadece. html falan ne gelirse düz yazı kalıyor
public final class Markdown {
    public enum Kind { TEXT, HEADING, LIST, QUOTE, CODE }
    public record Block(Kind kind, String text, String language) {}
    private Markdown() {}

    public static List<Block> parse(String source) {
        List<Block> out = new ArrayList<>();
        StringBuilder paragraph = new StringBuilder(); List<String> code = new ArrayList<>();
        String fence = null, language = "";

        for (String line : source.split("\n", -1)) {
            String trimmed = line.stripLeading();
            if (fence != null) {
                if (trimmed.stripTrailing().equals(fence)) {
                    out.add(new Block(Kind.CODE, String.join("\n", code), language)); code.clear(); fence = null;
                } else { code.add(line); }
                continue;
            }

            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                flush(out, paragraph); fence = trimmed.substring(0, 3); language = trimmed.substring(3).strip();
            } else if (trimmed.matches("#{1,6}\\s+.*")) {
                flush(out, paragraph); out.add(new Block(Kind.HEADING, trimmed.replaceFirst("^#{1,6}\\s+", ""), ""));
            } else if (trimmed.matches("(?:[-+*]|\\d+[.)])\\s+.*")) {
                flush(out, paragraph); out.add(new Block(Kind.LIST, trimmed.replaceFirst("^[-+*]\\s+", "• "), ""));
            } else if (trimmed.startsWith("> ")) {
                flush(out, paragraph); out.add(new Block(Kind.QUOTE, trimmed.substring(2), ""));
            } else if (line.isBlank()) { flush(out, paragraph); }
            else { if (!paragraph.isEmpty()) paragraph.append('\n'); paragraph.append(line); }
        }

        if (fence != null) out.add(new Block(Kind.CODE, String.join("\n", code), language)); // kapanmayan kod bloğu kalırsa onu da göster, yoksa yazı kayboluyor
        flush(out, paragraph); return out;
    }

    private static void flush(List<Block> out, StringBuilder text) {
        if (!text.isEmpty()) { out.add(new Block(Kind.TEXT, text.toString(), "")); text.setLength(0); }
    }
}
