package com.sentry.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 32, message = "Username cannot exceed 32 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain alphanumeric characters, hyphens, and underscores")
    private String username;

    @NotBlank(message = "Display name cannot be blank")
    @Size(max = 50, message = "Display name cannot exceed 50 characters")
    private String displayName;

    @NotBlank(message = "Password hash cannot be blank")
    @Size(max = 255, message = "Password hash cannot exceed 255 characters")
    private String passwordHash;
}
