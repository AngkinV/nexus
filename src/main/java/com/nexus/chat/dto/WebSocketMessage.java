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

        // Chat events
        CHAT_CREATED,
        CHAT_DISABLED,

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

        // Contact request events (好友申请事件)
        CONTACT_REQUEST,           // 收到好友申请
        CONTACT_REQUEST_ACCEPTED,  // 好友申请被接受
        CONTACT_REQUEST_REJECTED,  // 好友申请被拒绝

        // Error
        ERROR
    }
}
