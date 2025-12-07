package com.nexus.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for add contact request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddContactRequest {
    private Long userId;
    private Long contactUserId;
}
