package com.travelmate.users.controller;

import com.travelmate.users.dto.ChangePasswordRequest;
import com.travelmate.users.dto.PreferencesDto;
import com.travelmate.users.dto.UpdateProfileRequest;
import com.travelmate.users.dto.UserResponse;
import com.travelmate.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

// Tout ce qui concerne l'utilisateur CONNECTE.
// "Principal" est fourni par Spring Security : principal.getName() = l'email lu dans le jeton JWT.
@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // GET /api/users/me
    @GetMapping
    public UserResponse getMe(Principal principal) {
        return userService.getMe(principal.getName());
    }

    // PUT /api/users/me
    @PutMapping
    public UserResponse updateMe(Principal principal, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateMe(principal.getName(), request);
    }

    // PUT /api/users/me/password
    @PutMapping("/password")
    public void changePassword(Principal principal, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getName(), request);
    }

    // DELETE /api/users/me
    @DeleteMapping
    public void deleteMe(Principal principal) {
        userService.deleteMe(principal.getName());
    }

    // GET /api/users/me/preferences
    @GetMapping("/preferences")
    public PreferencesDto getPreferences(Principal principal) {
        return userService.getPreferences(principal.getName());
    }

    // PUT /api/users/me/preferences
    @PutMapping("/preferences")
    public PreferencesDto updatePreferences(Principal principal, @RequestBody PreferencesDto request) {
        return userService.updatePreferences(principal.getName(), request);
    }
}
