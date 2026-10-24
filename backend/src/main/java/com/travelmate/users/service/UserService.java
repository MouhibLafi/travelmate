package com.travelmate.users.service;

import com.travelmate.users.dto.ChangePasswordRequest;
import com.travelmate.users.dto.PreferencesDto;
import com.travelmate.users.dto.UpdateProfileRequest;
import com.travelmate.users.dto.UserResponse;
import com.travelmate.users.entity.Role;
import com.travelmate.users.entity.TravelPreferences;
import com.travelmate.users.entity.User;
import com.travelmate.exception.ApiException;
import com.travelmate.users.event.UserDeletedEvent;
import com.travelmate.users.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       ApplicationEventPublisher events) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
    }

    // ===================== Utilisateur connecte =====================

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    public UserResponse getMe(String email) {
        return UserResponse.from(findByEmail(email));
    }

    public UserResponse updateMe(String email, UpdateProfileRequest request) {
        User user = findByEmail(email);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        return UserResponse.from(userRepository.save(user));
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Le mot de passe actuel est incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteMe(String email) {
        delete(findByEmail(email));
    }

    // ===================== Preferences de voyage =====================

    public PreferencesDto getPreferences(String email) {
        return PreferencesDto.from(findByEmail(email).getPreferences());
    }

    public PreferencesDto updatePreferences(String email, PreferencesDto request) {
        User user = findByEmail(email);
        TravelPreferences prefs = user.getPreferences();

        prefs.setDescription(request.description());
        prefs.setInterests(request.interests() == null ? new HashSet<>() : new HashSet<>(request.interests()));
        prefs.setBudgetLevel(request.budgetLevel());
        prefs.setAiGenerated(request.aiGenerated());

        userRepository.save(user); // cascade : les preferences sont sauvegardees aussi
        return PreferencesDto.from(prefs);
    }

    // ===================== Administration =====================

    // Liste des utilisateurs, avec recherche optionnelle sur le nom / prenom / email
    public List<UserResponse> findAll(String search) {
        String text = search == null ? "" : search.trim().toLowerCase();
        return userRepository.findAll().stream()
                .filter(u -> text.isEmpty()
                        || u.getFirstName().toLowerCase().contains(text)
                        || u.getLastName().toLowerCase().contains(text)
                        || u.getEmail().contains(text))
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse changeRole(Long id, Role role, String adminEmail) {
        User user = findById(id);
        if (user.getEmail().equals(adminEmail)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vous ne pouvez pas modifier votre propre rôle");
        }
        user.setRole(role);
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse changeStatus(Long id, boolean active, String adminEmail) {
        User user = findById(id);
        if (user.getEmail().equals(adminEmail)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vous ne pouvez pas désactiver votre propre compte");
        }
        user.setActive(active);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id, String adminEmail) {
        User user = findById(id);
        if (user.getEmail().equals(adminEmail)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vous ne pouvez pas supprimer votre propre compte");
        }
        delete(user);
    }

    // Les autres modules suppriment d'abord leurs donnees (voir UserDeletedEvent), puis on supprime l'utilisateur.
    // @Transactional : si une etape echoue, rien n'est supprime.
    private void delete(User user) {
        events.publishEvent(new UserDeletedEvent(user.getId()));
        userRepository.delete(user);
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }
}
