package com.travelmate.itinerary.entity;

import jakarta.persistence.*;

import java.time.LocalTime;

// Un element d'une journee : activite, repas, hebergement, transport ou element libre
@Entity
@Table(name = "itinerary_items")
public class ItineraryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "day_id")
    private ItineraryDay day;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType type;

    // Id dans le catalogue (activite, restaurant ou hebergement), null pour un element libre
    private Long refId;

    @Column(nullable = false)
    private String name;
    private String category;

    private LocalTime startTime;
    private LocalTime endTime;

    private double unitPrice;    // prix d'une unite : un billet, un repas, une nuit
    private int quantity = 1;    // nombre d'unites, ex. nuits d'hotel
    private double estimatedCost;

    @Column(length = 1000)
    private String notes;

    public ItineraryItem() {
    }

    // Le cout est toujours calcule ici, jamais recopie depuis l'IA
    public void setPricing(double unitPrice, int quantity) {
        this.unitPrice = unitPrice;
        this.quantity = Math.max(1, quantity);
        this.estimatedCost = unitPrice * this.quantity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ItineraryDay getDay() { return day; }
    public void setDay(ItineraryDay day) { this.day = day; }

    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }

    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public double getUnitPrice() { return unitPrice; }

    public int getQuantity() { return quantity; }

    public double getEstimatedCost() { return estimatedCost; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
