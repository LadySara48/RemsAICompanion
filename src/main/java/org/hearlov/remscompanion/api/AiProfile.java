package org.hearlov.remscompanion.api;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
public final class AiProfile {
    public String id = java.util.UUID.randomUUID().toString(); // her profile ayrı id, sohbetler doğru profile bağlansın diye

    public enum Provider {
        COMPATIBLE("OpenAI compatible", "https://api.openai.com/v1"),
        GEMINI("Google Gemini", "https://generativelanguage.googleapis.com/v1beta"),
        GROK("xAI Grok", "https://api.x.ai/v1"),
        CLAUDE("Anthropic Claude", "https://api.anthropic.com/v1"),
        DEEPSEEK("DeepSeek", "https://api.deepseek.com"),
        KIMI("Kimi", "https://api.moonshot.ai/v1"),
        OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1"),
        VERCEL("Vercel AI Gateway", "https://ai-gateway.vercel.sh/v1");

        public final String label, baseUrl;
        Provider(String label, String baseUrl) { this.label = label; this.baseUrl = baseUrl; }
        public boolean supportsSearch() { return this == GEMINI || this == GROK || this == OPENROUTER || this == VERCEL; } // web arama bi tek bunlarda çalıştı
    }

    public String name = "Default";
    public Provider provider = Provider.COMPATIBLE;
    public String apiUrl = provider.baseUrl;
    public String apiKey = "";
    public String model = "";
    public boolean webSearch;
    // bu sadece isteğin o anki kopyası, seçili prompt artık ayrı PromptStore'dan geliyor
    public String systemPrompt = "";

    public AiProfile copy() {
        AiProfile p = new AiProfile();
        p.id = id;
        p.name = name; p.provider = provider; p.apiUrl = apiUrl; p.apiKey = apiKey;
        p.model = model; p.webSearch = webSearch; p.systemPrompt = systemPrompt;
        return p;
    }
}
