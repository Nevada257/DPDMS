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

    public WhatsAppSender(@Value("${alerts.whatsapp.api-url:https://graph.facebook.com/v21.0}") String apiUrl,
                          @Value("${alerts.whatsapp.phone-number-id:}") String phoneNumberId,
                          @Value("${alerts.whatsapp.token:}") String token) {
        this.apiUrl = apiUrl;
        this.phoneNumberId = phoneNumberId;
        this.token = token;
    }

    public boolean isConfigured() {
        return token != null && !token.isBlank() && phoneNumberId != null && !phoneNumberId.isBlank();
    }

    /** Sends the message; throws if the API does not accept it. */
    public void send(String phone, String text) throws IOException, InterruptedException {
        String to = phone.replaceAll("[^0-9]", ""); // API expects digits only, with country code
        String body = "{\"messaging_product\":\"whatsapp\",\"to\":\"" + to
                + "\",\"type\":\"text\",\"text\":{\"preview_url\":false,\"body\":\"" + json(text) + "\"}}";

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
