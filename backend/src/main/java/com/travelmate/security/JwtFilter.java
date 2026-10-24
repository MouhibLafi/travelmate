package com.travelmate.security;

import com.travelmate.users.entity.User;
import com.travelmate.users.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Ce filtre s'execute a CHAQUE requete :
// 1. il lit l'en-tete "Authorization: Bearer <jeton>"
// 2. il verifie le jeton et retrouve l'utilisateur
// 3. il dit a Spring Security "cet utilisateur est connecte, avec tel role"
// (Pas de @Component : il est cree dans SecurityConfig, sinon Spring l'ajouterait deux fois.)
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // on enleve "Bearer "
            String email = jwtService.extractEmail(token);

            if (email != null) {
                User user = userRepository.findByEmail(email).orElse(null);

                // On n'authentifie que les comptes qui existent et qui sont actifs
                if (user != null && user.isActive()) {
                    // "ROLE_ADMIN" ou "ROLE_VOYAGEUR" -> utilise par hasRole("ADMIN")
                    var authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
                    var authentication = new UsernamePasswordAuthenticationToken(email, null, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
