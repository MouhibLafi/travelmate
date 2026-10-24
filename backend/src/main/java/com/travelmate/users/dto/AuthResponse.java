package com.travelmate.users.dto;

// Reponse du login et de l'inscription : le jeton JWT + l'utilisateur
public record AuthResponse(String token, UserResponse user) {
}
