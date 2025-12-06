package com.nexus.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private MessageType type;
    private Object payload;
    
    public enum MessageType {
        CHAT_MESSAGE,
        USER_ONLINE,
        USER_OFFLINE,
        TYPING,
        MESSAGE_READ,
        ERROR
    }
}
