package com.nexus.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * WebSocket Channel Interceptor for user authentication
 * Extracts userId from STOMP CONNECT headers and sets it as the user Principal
 */
@Slf4j
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract userId from connect headers
            String userId = accessor.getFirstNativeHeader("userId");

            if (userId != null && !userId.isEmpty()) {
                // Set user principal
                accessor.setUser(new StompPrincipal(userId));
                log.info("WebSocket 用户连接: userId={}", userId);
            } else {
                log.warn("WebSocket 连接缺少 userId 头信息");
            }
        }

        return message;
    }

    /**
     * Simple Principal implementation for WebSocket users
     */
    private static class StompPrincipal implements Principal {
        private final String name;

        public StompPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
