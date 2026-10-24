package com.travelmate.users.repository;

import com.travelmate.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Spring Data genere automatiquement le code SQL a partir du nom des methodes.
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
