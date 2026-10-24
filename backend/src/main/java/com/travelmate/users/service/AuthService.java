package com.travelmate.users.service;

import com.travelmate.users.dto.AuthResponse;
import com.travelmate.users.dto.LoginRequest;
import com.travelmate.users.dto.RegisterRequest;
import com.travelmate.users.dto.UserResponse;
import com.travelmate.users.entity.Role;
import com.travelmate.users.entity.User;
import com.travelmate.exception.ApiException;
import com.travelmate.users.repository.UserRepository;
import com.travelmate.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Injection par constructeur : Spring fournit automatiquement ces objets
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Cet email est déjà utilisé");
        }

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password())); // on hache le mot de passe
        user.setRole(Role.VOYAGEUR);                                   // un nouvel inscrit est toujours voyageur
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect"));

        // On compare le mot de passe tape avec le mot de passe hache en base
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect");
        }

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Ce compte a été désactivé par un administrateur");
        }

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }
}
