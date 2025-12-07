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
        // Chat messages
        CHAT_MESSAGE,
        MESSAGE_READ,
        TYPING,

        // User status
        USER_ONLINE,
        USER_OFFLINE,
        USER_STATUS_CHANGED,

        // Group events
        GROUP_CREATED,
        GROUP_UPDATED,
        GROUP_DELETED,
        GROUP_MEMBER_JOINED,
        GROUP_MEMBER_LEFT,
        GROUP_MESSAGE,

        // Contact events
        CONTACT_ADDED,
        CONTACT_REMOVED,
        CONTACT_STATUS_CHANGED,

        // Error
        ERROR
    }
}
