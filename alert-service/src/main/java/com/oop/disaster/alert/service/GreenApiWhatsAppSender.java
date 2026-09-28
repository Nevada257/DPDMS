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
 * Sends WhatsApp alerts through Green API (green-api.com, free Developer plan):
 *   POST {GREENAPI_API_URL}/waInstance{GREENAPI_ID_INSTANCE}/sendMessage/{GREENAPI_API_TOKEN}
 *   body: {"chatId": "2637XXXXXXXX@c.us", "message": "..."}
 *
 * Green API links an ordinary WhatsApp account (scan a QR code in the Green API
 * console), so there is no 24-hour window, no template and no token that expires
 * every day. When the three GREENAPI_ settings are present it is used instead of
 * the Meta Cloud API (see AlertDispatcher).
 */
@Component
public class GreenApiWhatsAppSender {

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String apiUrl;
    private final String idInstance;
    private final String apiToken;

    public GreenApiWhatsAppSender(@Value("${alerts.greenapi.api-url:}") String apiUrl,
                                  @Value("${alerts.greenapi.id-instance:}") String idInstance,
                                  @Value("${alerts.greenapi.api-token:}") String apiToken) {
        this.apiUrl = apiUrl == null ? "" : apiUrl.trim().replaceAll("/+$", "");
        this.idInstance = idInstance == null ? "" : idInstance.trim();
        this.apiToken = apiToken == null ? "" : apiToken.trim();
    }

    public boolean isConfigured() {
        return !apiUrl.isEmpty() && !idInstance.isEmpty() && !apiToken.isEmpty();
    }

    /** Green API wants the number as digits with the country code, followed by "@c.us". */
    static String chatId(String phone) {
        return phone.replaceAll("[^0-9]", "") + "@c.us";
    }

    /** Sends a text message; throws if Green API does not accept it. */
    public void send(String phone, String subject, String text) throws IOException, InterruptedException {
        String message = "*" + subject + "*\n" + text;
        String body = "{\"chatId\":\"" + chatId(phone) + "\",\"message\":\"" + WhatsAppSender.json(message) + "\"}";

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(apiUrl + "/waInstance" + idInstance + "/sendMessage/" + apiToken))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Green API returned " + response.statusCode() + ": " + response.body());
        }
    }
}
