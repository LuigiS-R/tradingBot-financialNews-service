package com.ai_got_this.news_ingestion_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;

@Configuration
@Slf4j
public class WebSocketClientConfig {
    private AlpacaProperties alpacaProperties;

    public WebSocketClientConfig(AlpacaProperties alpacaProperties) {
        this.alpacaProperties = alpacaProperties;
    }

    @Bean
    public WebSocketConnectionManager webConnectionManager(AlpacaWebSocketHandler alpacaWebSocketHandler) {
        //Create a new HttpHeaders object and add both the alpacaAPI ID and alpacaAPIKey to it
        String apiKeyId = alpacaProperties.getKey();
        String apiSecretKey = alpacaProperties.getSecret();
        String newsStreamUrl = alpacaProperties.getNewsStreamUrl();
        if (!isValidConfiguration(apiKeyId, apiSecretKey, newsStreamUrl, alpacaProperties.getSubscriptionMessage())) {
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("APCA-API-KEY-ID", apiKeyId);
        headers.add("APCA-API-SECRET-KEY", apiSecretKey);

        WebSocketConnectionManager manager = new WebSocketConnectionManager(
                new StandardWebSocketClient(),
                alpacaWebSocketHandler,
                newsStreamUrl
        );

        manager.setAutoStartup(true);
        manager.setHeaders(headers);
        return manager;
    }

    private boolean isValidConfiguration(String apiKeyId, String apiSecretKey, String newsStreamUrl, String subscriptionMessage) {
        if (apiKeyId == null || apiKeyId.isBlank() || apiSecretKey == null || apiSecretKey.isBlank()) {
            log.error("Alpaca WebSocket credentials are not configured");
            return false;
        }

        if (newsStreamUrl == null || newsStreamUrl.isBlank()) {
            log.error("Alpaca WebSocket news stream URL is not configured");
            return false;
        }

        try {
            String scheme = URI.create(newsStreamUrl).getScheme();
            if (!"ws".equals(scheme) && !"wss".equals(scheme)) {
                log.error("Alpaca WebSocket news stream URL must use ws or wss");
                return false;
            }
        }
        catch (IllegalArgumentException e) {
            log.error("Alpaca WebSocket news stream URL is invalid");
            return false;
        }

        if (subscriptionMessage == null || subscriptionMessage.isBlank()) {
            log.error("Alpaca WebSocket subscription message is not configured");
            return false;
        }

        try {
            JsonNode subscription = new ObjectMapper().readTree(subscriptionMessage);
            if (!subscription.isObject()) {
                log.error("Alpaca WebSocket subscription message must be a JSON object");
                return false;
            }
            if (!"subscribe".equals(subscription.path("action").asString()) || !subscription.path("news").isArray()) {
                log.error("Alpaca WebSocket subscription message must subscribe to news");
                return false;
            }
        }
        catch (Exception e) {
            log.error("Alpaca WebSocket subscription message is invalid");
            return false;
        }

        return true;
    }
}
