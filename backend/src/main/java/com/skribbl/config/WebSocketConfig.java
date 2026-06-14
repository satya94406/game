package com.skribbl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * STOMP-over-SockJS wiring.
 *
 * <ul>
 *   <li>Clients open the SockJS connection at {@code /ws}.</li>
 *   <li>Client -&gt; server messages are sent to destinations prefixed {@code /app}
 *       (handled by {@code @MessageMapping} controllers).</li>
 *   <li>Server -&gt; client broadcasts go through the in-memory simple broker on
 *       {@code /topic}. Per-room traffic uses {@code /topic/room/{code}} and
 *       per-player traffic uses {@code /topic/room/{code}/u/{playerId}}.</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        // Drawing batches are small, but give comfortable headroom.
        registry.setMessageSizeLimit(128 * 1024);
        registry.setSendBufferSizeLimit(1024 * 1024);
        registry.setSendTimeLimit(20_000);
    }
}
