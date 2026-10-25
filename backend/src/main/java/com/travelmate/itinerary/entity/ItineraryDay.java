package com.travelmate.itinerary.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Une journee de l'itineraire, avec ses elements tries par heure
@Entity
@Table(name = "itinerary_days")
public class ItineraryDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "itinerary_id")
    private Itinerary itinerary;

    private int dayNumber;   // 1, 2, 3...
    private LocalDate date;
    private String title;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startTime ASC")
    private List<ItineraryItem> items = new ArrayList<>();

    public ItineraryDay() {
    }

    public ItineraryDay(int dayNumber, LocalDate date, String title) {
        this.dayNumber = dayNumber;
        this.date = date;
        this.title = title;
    }

    public void addItem(ItineraryItem item) {
        item.setDay(this);
        items.add(item);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Itinerary getItinerary() { return itinerary; }
    public void setItinerary(Itinerary itinerary) { this.itinerary = itinerary; }

    public int getDayNumber() { return dayNumber; }
    public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<ItineraryItem> getItems() { return items; }
    public void setItems(List<ItineraryItem> items) { this.items = items; }
}
