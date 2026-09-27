package com.oop.disaster.alert.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sends WhatsApp text messages through the WhatsApp Business Cloud API
 * (POST {api-url}/{phone-number-id}/messages). The access token and phone
 * number id come from WHATSAPP_TOKEN and WHATSAPP_PHONE_NUMBER_ID; when they
 * are not set, attempts are logged as SIMULATED.
 */
@Component
public class WhatsAppSender {

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String apiUrl;
    private final String phoneNumberId;
    private final String token;
    private final String templateName;
    private final String templateLanguage;

    public WhatsAppSender(@Value("${alerts.whatsapp.api-url:https://graph.facebook.com/v21.0}") String apiUrl,
                          @Value("${alerts.whatsapp.phone-number-id:}") String phoneNumberId,
                          @Value("${alerts.whatsapp.token:}") String token,
                          @Value("${alerts.whatsapp.fallback-template:hello_world}") String templateName,
                          @Value("${alerts.whatsapp.fallback-language:en_US}") String templateLanguage) {
        this.apiUrl = apiUrl;
        this.phoneNumberId = phoneNumberId;
        this.token = token;
        this.templateName = templateName;
        this.templateLanguage = templateLanguage;
    }

    public boolean isConfigured() {
        return token != null && !token.isBlank() && phoneNumberId != null && !phoneNumberId.isBlank();
    }

    /** Sends a free-text message; throws if the API does not accept it. */
    public void send(String phone, String text) throws IOException, InterruptedException {
        String body = "{\"messaging_product\":\"whatsapp\",\"to\":\"" + digits(phone)
                + "\",\"type\":\"text\",\"text\":{\"preview_url\":false,\"body\":\"" + json(text) + "\"}}";
        post(body);
    }

    /**
     * Sends a pre-approved template message. WhatsApp only delivers free text to a
     * number that has messaged the business in the last 24 hours; outside that window
     * only an approved template gets through, so this is the fallback.
     */
    public void sendTemplate(String phone) throws IOException, InterruptedException {
        String body = "{\"messaging_product\":\"whatsapp\",\"to\":\"" + digits(phone)
                + "\",\"type\":\"template\",\"template\":{\"name\":\"" + json(templateName)
                + "\",\"language\":{\"code\":\"" + json(templateLanguage) + "\"}}}";
        post(body);
    }

    /** True if the API refused free text because the 24-hour conversation window is closed. */
    public static boolean isOutsideConversationWindow(Exception e) {
        String m = e.getMessage() == null ? "" : e.getMessage();
        return m.contains("131047") || m.contains("131026") || m.toLowerCase().contains("re-engagement");
    }

    private static String digits(String phone) {
        return phone.replaceAll("[^0-9]", ""); // API expects digits only, with country code
    }

    private void post(String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl + "/" + phoneNumberId + "/messages"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("WhatsApp API returned " + response.statusCode() + ": " + response.body());
        }
    }

    static String json(String s) {
        StringBuilder out = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
