package org.hearlov.remscompanion.api;

import com.google.gson.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
// burda sadece sağlayıcı protokolleri ve http var. minecraft class'ı yok, loglara key yazmıyorum
public final class AiClient {
    public interface Listener {
        void text(String delta);
        void source(String url);
        void done();
        void error(String code);
        default void usage(AiUsage usage) {}
    }

    public static final class Call {
        private volatile boolean cancelled;
        private volatile boolean timedOut;
        private volatile InputStream body;
        private volatile Thread worker;

        public void cancel() {
            cancelled = true;
            if (worker != null) worker.interrupt();
            try { if (body != null) body.close(); } catch (IOException ignored) {}
        }
        private void timeout() { timedOut = true; cancel(); }
    }

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NEVER).build();
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rem-ai-timeout"); t.setDaemon(true); return t;
    });

    public record Request(URI uri, JsonObject body, Map<String, String> headers) {}

    public static Request request(AiProfile p, List<AiMessage> history) {
        if (p.provider == null || p.model == null || !p.model.matches("[a-zA-Z0-9._:/-]+"))
            throw new IllegalArgumentException("model");
        URI base = URI.create(p.apiUrl.strip().replaceAll("/+$", ""));
        if (base.getHost() == null || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null
                || !("https".equals(base.getScheme()) || "http".equals(base.getScheme())))
            throw new IllegalArgumentException("url");
        if (p.webSearch && !p.provider.supportsSearch()) throw new IllegalArgumentException("search");

        JsonObject body = new JsonObject();
        JsonArray messages = new JsonArray();
        for (AiMessage message : history) {
            if (!message.content().isEmpty()) messages.add(obj("role", message.role(), "content", message.content()));
        }

        Map<String, String> headers = new HashMap<>();
        String path;

        if (p.provider == AiProfile.Provider.GEMINI) {
            path = "/models/" + p.model.replaceFirst("^models/", "") + ":streamGenerateContent?alt=sse";
            headers.put("x-goog-api-key", p.apiKey); // gemini bearer istemiyor, kendi header'ı var
            JsonArray contents = new JsonArray();
            for (AiMessage message : history) {
                if (message.content().isEmpty()) continue;
                JsonObject content = obj("role", message.role().equals("assistant") ? "model" : "user");
                content.add("parts", array(obj("text", message.content()))); contents.add(content);
            }
            body.add("contents", contents);
            if (!p.systemPrompt.isBlank()) {
                JsonObject system = new JsonObject(); system.add("parts", array(obj("text", p.systemPrompt)));
                body.add("systemInstruction", system);
            }
            if (p.webSearch) { JsonObject tool = new JsonObject(); tool.add("google_search", new JsonObject()); body.add("tools", array(tool)); }
        } else {
            body.addProperty("model", p.model); body.addProperty("stream", true);
            headers.put("Authorization", "Bearer " + p.apiKey);

            if (p.provider == AiProfile.Provider.CLAUDE) {
                path = "/messages"; headers.clear(); headers.put("x-api-key", p.apiKey);
                headers.put("anthropic-version", "2023-06-01");
                body.addProperty("max_tokens", 4096);
                if (!p.systemPrompt.isBlank()) body.addProperty("system", p.systemPrompt);
                body.add("messages", messages);
            } else if (p.provider == AiProfile.Provider.GROK) {
                path = "/responses"; body.addProperty("store", false);
                if (!p.systemPrompt.isBlank()) body.addProperty("instructions", p.systemPrompt);
                body.add("input", messages);
                if (p.webSearch) body.add("tools", array(obj("type", "web_search")));
            } else {
                path = "/chat/completions";
                if (!p.systemPrompt.isBlank()) {
                    JsonArray withSystem = array(obj("role", "system", "content", p.systemPrompt));
                    withSystem.addAll(messages); messages = withSystem;
                }
                body.add("messages", messages);
            }
        }

        if (body.has("messages") && p.provider != AiProfile.Provider.CLAUDE) {
            JsonObject options = new JsonObject(); options.addProperty("include_usage", true); body.add("stream_options", options);
        }
        if (p.provider == AiProfile.Provider.OPENROUTER) { JsonObject usage = new JsonObject(); usage.addProperty("include", true); body.add("usage", usage); }
        if (p.webSearch && p.provider == AiProfile.Provider.OPENROUTER) {
            JsonObject tool = obj("type", "openrouter:web_search");
            JsonObject parameters = obj("engine", "auto");
            parameters.addProperty("max_results", 5);
            tool.add("parameters", parameters);
            body.add("tools", array(tool)); body.addProperty("max_tool_calls", 3);
        } else if (p.webSearch && p.provider == AiProfile.Provider.VERCEL) {
            String query = "";
            for (AiMessage message : history) if (message.role().equals("user")) query = message.content();
            if (query.isBlank()) throw new IllegalArgumentException("search_query");
            JsonObject config = obj("query", query); config.addProperty("max_results", 5);
            JsonObject tool = obj("type", "vercel:perplexity_search"); tool.add("config", config);
            body.add("tools", array(tool)); body.addProperty("tool_choice", "auto");
        }

        return new Request(URI.create(base + path), body, headers);
    }

    public static Call stream(AiProfile profile, List<AiMessage> history, Listener listener) {
        Request spec = request(profile, history);
        Call call = new Call();

        call.worker = RequestThreads.unstarted(() -> {
            AiUsage usage = new AiUsage();
            ScheduledFuture<?> timeout = TIMER.schedule(call::timeout, 180, TimeUnit.SECONDS);

            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder(spec.uri())
                        .timeout(Duration.ofSeconds(180)).header("Content-Type", "application/json")
                        .header("Accept", "text/event-stream")
                        .POST(HttpRequest.BodyPublishers.ofString(spec.body().toString(), StandardCharsets.UTF_8));
                spec.headers().forEach(builder::header);
                HttpResponse<InputStream> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
                call.body = response.body();
                try (InputStream body = call.body) {
                    if (call.cancelled) return;
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        listener.error(ApiErrors.classify(response.statusCode(), new String(body.readNBytes(65_536), StandardCharsets.UTF_8))); return;
                    }
                    if (!response.headers().firstValue("content-type").orElse("").contains("text/event-stream")) {
                        listener.error("unexpected_response"); return;
                    }

                    boolean[] terminal = {false}, finishedText = {false}; int[] length = {0};
                    SseReader.read(new InputStreamReader(body, StandardCharsets.UTF_8), data -> {
                        if (call.cancelled || terminal[0]) return;
                        if (data.equals("[DONE]")) { terminal[0] = true; return; }
                        JsonObject event = JsonParser.parseString(data).getAsJsonObject();
                        usage.accept(profile.provider, event);
                        if (event.has("error") || str(event, "type").equals("error") || str(event, "type").equals("response.failed"))
                            throw new IllegalStateException(ApiErrors.classify(0, event.toString()));
                        parse(profile.provider, event, delta -> {
                            length[0] += delta.length();
                            if (length[0] > 1_000_000) throw new IllegalStateException("response_too_large"); // cevap çok uzarsa kes, hafıza şişmesin
                            listener.text(delta);
                        }, listener::source);
                        String type = str(event, "type");
                        if (type.equals("message_stop") || type.equals("response.completed")) terminal[0] = true;
                        if (type.equals("response.incomplete")) throw new IllegalStateException("incomplete_response");
                        if (event.has("choices")) for (JsonElement item : event.getAsJsonArray("choices")) {
                            String reason = str(item.getAsJsonObject(), "finish_reason");
                            if (!reason.isEmpty()) {
                                if (!reason.equals("stop")) throw new IllegalStateException(reason.equals("tool_calls") ? "api_tool_execution" : "incomplete_response");
                                finishedText[0] = true;
                            }
                        }

                        if (event.has("promptFeedback") && event.getAsJsonObject("promptFeedback").has("blockReason"))
                            throw new IllegalStateException("blocked_response");
                        if (event.has("candidates")) for (JsonElement item : event.getAsJsonArray("candidates")) {
                            String reason = str(item.getAsJsonObject(), "finishReason");
                            if (!reason.isEmpty()) {
                                if (!reason.equals("STOP")) throw new IllegalStateException("incomplete_response");
                                terminal[0] = true;
                            }
                        }
                    }, () -> terminal[0] || call.cancelled);

                    if (!call.cancelled) {
                        if (!terminal[0] && !finishedText[0]) listener.error("incomplete_response");
                        else if (length[0] == 0) listener.error("empty_response");
                        else listener.done();
                    }
                }
            } catch (Exception e) {
                if (!call.cancelled) listener.error(e instanceof IllegalStateException ? e.getMessage() : "connection_error");
            } finally {
                timeout.cancel(false);
                listener.usage(usage);
                if (call.cancelled) listener.error(call.timedOut ? "request_timed_out" : "cancelled");
            }
        });

        call.worker.start();
        return call;
    }

    public static void parse(AiProfile.Provider provider, JsonObject event, Consumer<String> text, Consumer<String> source) {
        if (provider == AiProfile.Provider.GEMINI) {
            if (!event.has("candidates")) return;
            JsonArray candidates = event.getAsJsonArray("candidates");
            if (candidates.isEmpty()) return;
            JsonObject candidate = candidates.get(0).getAsJsonObject();
            if (candidate.has("content")) for (JsonElement part : candidate.getAsJsonObject("content").getAsJsonArray("parts")) {
                JsonObject p = part.getAsJsonObject();
                if (!p.has("thought") || !p.get("thought").getAsBoolean()) emit(p, "text", text);
            }
            if (candidate.has("groundingMetadata")) sources(candidate.get("groundingMetadata"), source);
        } else if (provider == AiProfile.Provider.GROK) {
            if (str(event, "type").equals("response.output_text.delta")) emit(event, "delta", text);
            if (event.has("annotation")) sources(event.get("annotation"), source);
            if (str(event, "type").equals("response.completed") && event.has("response")) sources(event.get("response"), source);
        } else if (provider == AiProfile.Provider.CLAUDE) {
            if (event.has("delta")) {
                JsonObject delta = event.getAsJsonObject("delta");
                if (str(delta, "type").equals("text_delta")) emit(delta, "text", text);
                if (delta.has("stop_reason") && !Set.of("end_turn", "stop_sequence").contains(str(delta, "stop_reason")))
                    throw new IllegalStateException("incomplete_response");
            }
        } else if (event.has("choices") && !event.getAsJsonArray("choices").isEmpty()) {
            JsonObject choice = event.getAsJsonArray("choices").get(0).getAsJsonObject();
            if (choice.has("delta")) {
                JsonObject delta = choice.getAsJsonObject("delta"); emit(delta, "content", text);
                if (delta.has("annotations")) sources(delta.get("annotations"), source);
            }
            if (choice.has("message")) {
                JsonObject message = choice.getAsJsonObject("message");
                if (message.has("annotations")) sources(message.get("annotations"), source);
            }
        }
    }

    private static void sources(JsonElement element, Consumer<String> source) {
        if (element.isJsonArray()) { for (JsonElement child : element.getAsJsonArray()) sources(child, source); }
        else if (element.isJsonObject()) for (var entry : element.getAsJsonObject().entrySet()) {
            if ((entry.getKey().equals("uri") || entry.getKey().equals("url")) && entry.getValue().isJsonPrimitive()) {
                String url = entry.getValue().getAsString();
                if (url.startsWith("https://") || url.startsWith("http://")) source.accept(url);
            } else sources(entry.getValue(), source);
        }
    }

    private static void emit(JsonObject object, String key, Consumer<String> output) {
        String value = str(object, key); if (!value.isEmpty()) output.accept(value);
    }

    private static String str(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }

    private static JsonObject obj(String... fields) {
        JsonObject result = new JsonObject();
        for (int i = 0; i < fields.length; i += 2) result.addProperty(fields[i], fields[i + 1]);
        return result;
    }

    private static JsonArray array(JsonElement value) { JsonArray a = new JsonArray(); a.add(value); return a; }
}


