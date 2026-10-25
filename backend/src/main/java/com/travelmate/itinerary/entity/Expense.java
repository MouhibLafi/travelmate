package com.travelmate.itinerary.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

// Argent reellement depense pendant le voyage (suivi du budget)
@Entity
@Table(name = "itinerary_expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_id")
    private Itinerary itinerary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType category;

    private double amount;   // en euros
    private LocalDate date;
    private String description;

    public Expense() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Itinerary getItinerary() { return itinerary; }
    public void setItinerary(Itinerary itinerary) { this.itinerary = itinerary; }

    public ItemType getCategory() { return category; }
    public void setCategory(ItemType category) { this.category = category; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
