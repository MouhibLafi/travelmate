package com.travelmate.itinerary.entity;

import com.travelmate.users.entity.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 * MODULE 6 - Itineraire et budget
 * Un itineraire jour par jour pour un voyage du module 2, avec son cout estime et ses depenses reelles.
 *   - il appartient a un User (module 1) : supprime avec lui (voir ItineraryService.onUserDeleted)
 *   - le voyage est garde par son id, sans cle etrangere : supprimer un voyage dans le module 2
 *     ne plante pas, l'itineraire devenu orphelin est supprime a la prochaine lecture
 */
@Entity
@Table(name = "itineraries")
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    // Voyage du module 2
    @Column(nullable = false)
    private Long tripId;

    // Copies du voyage : l'itineraire et son PDF restent lisibles seuls
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private double budget;              // en euros

    private double totalEstimatedCost;  // somme des elements, recalculee par recalculateTotal()
    private boolean generatedByAi;      // false = plan construit par les regles (IA indisponible)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC")
    private List<ItineraryDay> days = new ArrayList<>();

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("date ASC")
    private List<Expense> expenses = new ArrayList<>();

    public Itinerary() {
    }

    public void addDay(ItineraryDay day) {
        day.setItinerary(this);
        days.add(day);
    }

    // Tous les elements, tous jours confondus
    public List<ItineraryItem> allItems() {
        return days.stream().flatMap(d -> d.getItems().stream()).toList();
    }

    public void recalculateTotal() {
        totalEstimatedCost = allItems().stream().mapToDouble(ItineraryItem::getEstimatedCost).sum();
    }

    public boolean isOverBudget() {
        return totalEstimatedCost > budget;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }

    public double getTotalEstimatedCost() { return totalEstimatedCost; }
    public void setTotalEstimatedCost(double totalEstimatedCost) { this.totalEstimatedCost = totalEstimatedCost; }

    public boolean isGeneratedByAi() { return generatedByAi; }
    public void setGeneratedByAi(boolean generatedByAi) { this.generatedByAi = generatedByAi; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<ItineraryDay> getDays() { return days; }
    public void setDays(List<ItineraryDay> days) { this.days = days; }

    public List<Expense> getExpenses() { return expenses; }
    public void setExpenses(List<Expense> expenses) { this.expenses = expenses; }
}
