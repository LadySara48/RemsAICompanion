package org.hearlov.remscompanion.api;

import com.google.gson.*;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
// sağlayıcının biriken sayaçları. stream'deki ara değerleri sakın üst üste toplama
public final class AiUsage {
    public long input, output, total;
    public double reportedUsd;
    public boolean reported, costReported;

    public void accept(AiProfile.Provider provider, JsonObject event) {
        JsonObject usage = object(event, "usage");
        if (event.has("usageMetadata")) {
            usage = object(event, "usageMetadata");
            if (usage == null) return;
            input = Math.max(input, number(usage, "promptTokenCount"));
            output = Math.max(output, number(usage, "candidatesTokenCount") + number(usage, "thoughtsTokenCount"));
            total = Math.max(total, number(usage, "totalTokenCount")); reported |= usage.has("totalTokenCount") || usage.has("promptTokenCount") || usage.has("candidatesTokenCount");
        } else {
            if (event.has("message") && event.get("message").isJsonObject()) usage = object(event.getAsJsonObject("message"), "usage");
            if (event.has("response") && event.get("response").isJsonObject()) usage = object(event.getAsJsonObject("response"), "usage");
            if (usage == null) return;
            input = Math.max(input, Math.max(number(usage, "prompt_tokens"), number(usage, "input_tokens"))
                    + (provider == AiProfile.Provider.CLAUDE ? number(usage, "cache_read_input_tokens") + number(usage, "cache_creation_input_tokens") : 0));
            output = Math.max(output, Math.max(number(usage, "completion_tokens"), number(usage, "output_tokens")));
            total = Math.max(total, number(usage, "total_tokens"));
            reported |= usage.has("prompt_tokens") || usage.has("input_tokens") || usage.has("completion_tokens") || usage.has("output_tokens") || usage.has("total_tokens");
            if (provider == AiProfile.Provider.OPENROUTER && usage.has("cost") && usage.get("cost").isJsonPrimitive()) {
                double amount = usage.get("cost").getAsDouble();
                if (Double.isFinite(amount) && amount >= 0) { reportedUsd = Math.max(reportedUsd, amount); costReported = true; }
            }
        }

        total = Math.max(total, input + output); // bazıları total yollamıyor, en az girdi+çıktı kadar olsun
    }

    private static JsonObject object(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonObject() ? object.getAsJsonObject(key) : null;
    }

    private static long number(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) return 0;
        try { return Math.max(0, object.get(key).getAsLong()); } catch (RuntimeException ex) { return 0; } // boşsa patlamasın, 0
    }
}
