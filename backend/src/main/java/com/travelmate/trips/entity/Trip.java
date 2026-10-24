package com.travelmate.trips.entity;

import com.travelmate.users.entity.BudgetLevel;
import com.travelmate.users.entity.Interest;
import com.travelmate.users.entity.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/*
 * MODULE 2 - Planification des voyages
 * Exemple d'integration avec le module 1 :
 *   - un voyage appartient a un User (entite du module 1)
 *   - il reutilise les enums Interest et BudgetLevel (meme vocabulaire partout)
 */
@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lien vers l'utilisateur du module 1.
    // Si l'utilisateur est supprime, ses voyages le sont aussi (voir TripService.onUserDeleted).
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;

    // Budget en euros
    private Double budget;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "trip_interests", joinColumns = @JoinColumn(name = "trip_id"))
    @Column(name = "interest")
    private Set<Interest> interests = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private BudgetLevel budgetLevel;

    public Trip() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }

    public Set<Interest> getInterests() { return interests; }
    public void setInterests(Set<Interest> interests) { this.interests = interests; }

    public BudgetLevel getBudgetLevel() { return budgetLevel; }
    public void setBudgetLevel(BudgetLevel budgetLevel) { this.budgetLevel = budgetLevel; }
}
