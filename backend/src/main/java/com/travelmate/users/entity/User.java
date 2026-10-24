package com.travelmate.users.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    // Mot de passe hache avec BCrypt (jamais en clair)
    private String password;

    private String phone;

    @Enumerated(EnumType.STRING)
    private Role role = Role.VOYAGEUR;

    // false = compte desactive par l'admin
    private boolean active = true;

    // Chaque utilisateur a UNE fiche de preferences.
    // cascade ALL : sauvegarder / supprimer le user fait pareil pour ses preferences.
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "preferences_id")
    private TravelPreferences preferences = new TravelPreferences();

    public User() {
    }

    // ---------- Getters et setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public TravelPreferences getPreferences() { return preferences; }
    public void setPreferences(TravelPreferences preferences) { this.preferences = preferences; }
}
