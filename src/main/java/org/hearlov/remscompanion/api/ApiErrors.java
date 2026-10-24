package org.hearlov.remscompanion.api;

import java.util.Locale;

/*
 * █   █  █████   ███   ████   █      ███   █   █
 * █   █  █      █   █  █   █  █     █   █  █   █
 * █████  ████   █████  ████   █     █   █  █   █
 * █   █  █      █   █  █  █   █     █   █   █ █
 * █   █  █████  █   █  █   █  █████   ███     █
 */
public final class ApiErrors {
    private ApiErrors() {}

    public static String classify(int status, String diagnostic) {
        String text = diagnostic.toLowerCase(Locale.ROOT); // hepsini küçült, büyük/küçük harf derdi olmasın

        if (status == 401) return "api_auth";
        if (status == 429 || text.contains("rate_limit")) return "api_rate";
        if (status == 402 || text.contains("insufficient") || text.contains("billing") || text.contains("credit") || text.contains("quota")) return "api_balance";
        if (text.contains("tool") && (text.contains("support") || text.contains("invalid") || text.contains("not allowed"))) return "api_tools";
        if (text.contains("context") && (text.contains("length") || text.contains("limit") || text.contains("exceed"))) return "api_context";
        if (status == 404 || text.contains("model_not_found") || text.contains("model not found")) return "api_model";
        if (status == 403) return "api_access";
        if (status >= 500) return "api_unavailable";
        if (text.contains("tool")) return "api_tool_execution"; // kalan tool hataları buraya düşüyor
        if (status == 400 || status == 422) return "api_request";
        return "provider_error";
    }
}
