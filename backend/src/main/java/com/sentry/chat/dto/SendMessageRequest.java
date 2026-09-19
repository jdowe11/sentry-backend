package com.sentry.chat.dto;

import jakarta.validation.constraints.NotBlank;
// import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {

    @NotBlank(message = "Ciphertext is required")
    // @Size(min = 40, max = 12000, message = "Ciphertext length must be between {min} and {max} characters") Will be worrying about length AFTER adding encryption.
    private String ciphertext;
}
