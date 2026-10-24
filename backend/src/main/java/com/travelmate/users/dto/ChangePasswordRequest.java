package com.travelmate.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Le mot de passe actuel est obligatoire") String currentPassword,
        @Size(min = 6, message = "Le nouveau mot de passe doit contenir au moins 6 caractères") String newPassword
) {
}
