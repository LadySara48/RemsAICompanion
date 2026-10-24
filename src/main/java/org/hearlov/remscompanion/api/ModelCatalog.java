package org.hearlov.remscompanion.api;

import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
public final class ModelCatalog {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    private ModelCatalog() {}

    public static CompletableFuture<List<String>> load(AiProfile profile) {
        AiProfile p = profile.copy();
        return CompletableFuture.supplyAsync(() -> {
            try {
                String base = p.apiUrl.strip().replaceAll("/+$", "");
                TreeSet<String> models = new TreeSet<>(); String next = "";

                for (int page = 0; page < 20; page++) { // en fazla 20 sayfa, sonsuza kadar dönmesin
                    String path = p.provider == AiProfile.Provider.GEMINI ? "/models?pageSize=1000" + (next.isEmpty() ? "" : "&pageToken=" + java.net.URLEncoder.encode(next, StandardCharsets.UTF_8)) : "/models";
                    URI uri = URI.create(base + path);
                    if (uri.getHost() == null || uri.getUserInfo() != null || !(uri.getScheme().equals("http") || uri.getScheme().equals("https"))) throw new IllegalArgumentException("invalid_connection");
                    var builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20)).GET();
                    if (p.provider == AiProfile.Provider.GEMINI) builder.header("x-goog-api-key", p.apiKey);
                    else if (p.provider == AiProfile.Provider.CLAUDE) builder.header("x-api-key", p.apiKey).header("anthropic-version", "2023-06-01");
                    else builder.header("Authorization", "Bearer " + p.apiKey);
                    var response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
                    String data;
                    try (var body = response.body()) { byte[] bytes = body.readNBytes(8_000_001); if (bytes.length > 8_000_000) throw new IllegalStateException("model_list_failed"); data = new String(bytes, StandardCharsets.UTF_8); }
                    if (response.statusCode() != 200) throw new IllegalStateException(ApiErrors.classify(response.statusCode(), data));
                    JsonObject root = JsonParser.parseString(data).getAsJsonObject();
                    models.addAll(parse(p.provider, root));
                    next = root.has("nextPageToken") ? root.get("nextPageToken").getAsString() : "";
                    if (next.isEmpty() || p.provider != AiProfile.Provider.GEMINI) break; // sayfalama zaten sadece gemini'de var, diğerleri tek seferde
                }

                return List.copyOf(models);
            } catch (Exception ex) { throw new CompletionException(new IllegalStateException(ex instanceof IllegalStateException ? ex.getMessage() : "model_list_failed")); }
        }, RequestThreads::start);
    }

    public static List<String> parse(AiProfile.Provider provider, JsonObject root) {
        TreeSet<String> result = new TreeSet<>(); String key = provider == AiProfile.Provider.GEMINI ? "models" : "data";
        if (!root.has(key) || !root.get(key).isJsonArray()) return List.of();
        for (JsonElement element : root.getAsJsonArray(key)) {
            JsonObject model = element.getAsJsonObject();
            if (provider == AiProfile.Provider.GEMINI && model.has("supportedGenerationMethods")) {
                boolean text = false;
                for (var method : model.getAsJsonArray("supportedGenerationMethods")) if (method.getAsString().contains("generateContent") || method.getAsString().contains("streamGenerateContent")) text = true;
                if (!text) continue;
            }

            String field = provider == AiProfile.Provider.GEMINI ? "name" : "id";
            if (model.has(field)) result.add(model.get(field).getAsString().replaceFirst("^models/", ""));
        }

        return List.copyOf(result);
    }
}
