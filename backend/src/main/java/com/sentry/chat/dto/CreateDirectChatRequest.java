package com.sentry.chat.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDirectChatRequest {

    @NotNull(message = "Recipient ID is required")
    @Min(value = 1, message = "Recipient ID must be a positive number")
    private Long recipientId;
}
