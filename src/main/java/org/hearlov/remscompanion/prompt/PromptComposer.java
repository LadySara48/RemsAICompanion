package org.hearlov.remscompanion.prompt;

import java.util.*;
import java.util.regex.*;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
public final class PromptComposer {
    public static final int MAX_BODY = 19_800;
    public static final int MAX_TOTAL = 21_450;
    private static final Pattern VARIABLE = Pattern.compile("%(name|age|player_name|language|aibehavour)%");

    private PromptComposer() {}

    public record GameContext(String dimension, int x, int y, int z, String server, String version, String loader) {
        public String block() {
            return "[GAME CONTENT]\nDimension: " + clean(dimension) + "\nPosition: X=" + x + ", Y=" + y + ", Z=" + z
                    + "\nServer: " + clean(server) + "\nVersion: Java Edition " + clean(version)
                    + "\nLoader: " + clean(loader) + "\n[/GAME CONTENT]";
        }
    }

    private static String clean(String value) { return value.replace('\n', ' ').replace('\r', ' '); } // satır sonları oyun bilgisi bloğunu bozuyordu, boşluğa çevirdim

    public static String body(String template, PromptStore.Variables v, String player, String language) {
        if (template.length() > MAX_BODY) throw new IllegalArgumentException("prompt_too_large");
        Map<String, String> values = Map.of("name", v.name, "age", v.age,
                "player_name", v.playerName.isBlank() ? player : v.playerName,
                "language", v.language.isBlank() ? language : v.language,
                "aibehavour", v.behaviour.isBlank() ? "None" : v.behaviour);
        Matcher matcher = VARIABLE.matcher(template); StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(values.get(matcher.group(1))));
            if (out.length() > MAX_BODY) throw new IllegalArgumentException("prompt_too_large");
        }

        matcher.appendTail(out);
        if (out.length() > MAX_BODY) throw new IllegalArgumentException("prompt_too_large");
        return out.toString();
    }

    public static String compose(String template, PromptStore.Variables variables, String player, String language, GameContext game) {
        String result = body(template, variables, player, language) + "\n\n" + game.block();
        if (result.length() > MAX_TOTAL) throw new IllegalArgumentException("prompt_too_large");
        return result;
    }

    // sunucu adı, adres, hostname, ip... kullanıcının girdiği hiçbiri buradan dışarı çıkmıyor
    public static String serverLabel(String address, boolean singleplayer) {
        if (singleplayer) return "Singleplayer";
        String host = address == null ? "" : address.toLowerCase(Locale.ROOT).strip();
        int colon = host.indexOf(':'); if (colon >= 0) host = host.substring(0, colon);
        if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        if (host.equals("hypixel.net") || host.endsWith(".hypixel.net")) return "Hypixel";
        if (host.equals("cubecraft.net") || host.endsWith(".cubecraft.net")) return "CubeCraft";
        return "Multiplayer (address hidden)"; // adres modele gitmesin
    }
}
