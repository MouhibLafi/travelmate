package com.travelmate.users.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "travel_preferences")
public class TravelPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // La phrase ou l'utilisateur se decrit (c'est l'entree de l'IA)
    @Column(length = 500)
    private String description;

    // Plusieurs centres d'interet -> table "preference_interests"
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "preference_interests", joinColumns = @JoinColumn(name = "preferences_id"))
    @Column(name = "interest")
    private Set<Interest> interests = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private BudgetLevel budgetLevel;

    // true si l'utilisateur a applique la suggestion de l'IA
    private boolean aiGenerated = false;

    public TravelPreferences() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<Interest> getInterests() { return interests; }
    public void setInterests(Set<Interest> interests) { this.interests = interests; }

    public BudgetLevel getBudgetLevel() { return budgetLevel; }
    public void setBudgetLevel(BudgetLevel budgetLevel) { this.budgetLevel = budgetLevel; }

    public boolean isAiGenerated() { return aiGenerated; }
    public void setAiGenerated(boolean aiGenerated) { this.aiGenerated = aiGenerated; }
}
