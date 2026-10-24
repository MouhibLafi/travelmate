package com.travelmate.users.config;

import com.travelmate.users.entity.BudgetLevel;
import com.travelmate.users.entity.Interest;
import com.travelmate.users.entity.Role;
import com.travelmate.users.entity.User;
import com.travelmate.users.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

// S'execute au demarrage : cree des comptes de test si la base est vide.
//   admin@travelmate.tn  / admin123     (ADMIN)
//   yasmine@example.com  / password123  (VOYAGEUR)
//   karim@example.com    / password123  (VOYAGEUR)
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = createUser("Admin", "TravelMate", "admin@travelmate.tn", "admin123", Role.ADMIN);
        userRepository.save(admin);

        User yasmine = createUser("Yasmine", "Trabelsi", "yasmine@example.com", "password123", Role.VOYAGEUR);
        yasmine.getPreferences().setDescription("Je suis étudiante, j'adore la plage et la street food.");
        yasmine.getPreferences().setInterests(new HashSet<>(Set.of(Interest.PLAGE, Interest.GASTRONOMIE)));
        yasmine.getPreferences().setBudgetLevel(BudgetLevel.ECONOMIQUE);
        userRepository.save(yasmine);

        User karim = createUser("Karim", "Ben Salah", "karim@example.com", "password123", Role.VOYAGEUR);
        userRepository.save(karim);
    }

    private User createUser(String firstName, String lastName, String email, String password, Role role) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        return user;
    }
}
