package com.travelmate.users.dto;

import com.travelmate.users.entity.Role;
import com.travelmate.users.entity.User;

// Ce que l'API renvoie pour un utilisateur : on ne renvoie JAMAIS le mot de passe.
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        Role role,
        boolean active
) {
    // Transforme une entite User en UserResponse
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getPhone(), user.getRole(), user.isActive());
    }
}
