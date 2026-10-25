package com.travelmate.accommodation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A bookable place to stay (hotel, apartment, hostel or guesthouse).
 * Other TravelMate modules are referenced only by plain fields (city/country, optional destinationId).
 */
@Entity
@Table(name = "accommodations", indexes = {
        @Index(name = "idx_accommodation_city", columnList = "city"),
        @Index(name = "idx_accommodation_country", columnList = "country"),
        @Index(name = "idx_accommodation_price", columnList = "price_per_night")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Accommodation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccommodationType type;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(length = 255)
    private String address;

    /** Official classification, 1 to 5. */
    @Column(nullable = false)
    private Integer stars;

    /** Average guest rating, 0 to 5. */
    @Column(nullable = false)
    private Double rating;

    @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    @Column(name = "max_guests", nullable = false)
    private Integer maxGuests;

    /** Optional link to the Destinations module (no foreign key on purpose). */
    @Column(name = "destination_id")
    private Long destinationId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "accommodation_amenities", joinColumns = @JoinColumn(name = "accommodation_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "amenity", length = 30, nullable = false)
    @BatchSize(size = 50)
    @Builder.Default
    private Set<Amenity> amenities = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "accommodation_images", joinColumns = @JoinColumn(name = "accommodation_id"))
    @OrderColumn(name = "position")
    @Column(name = "url", length = 500, nullable = false)
    @BatchSize(size = 50)
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @OneToMany(mappedBy = "accommodation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookedPeriod> periods = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addPeriod(BookedPeriod period) {
        periods.add(period);
        period.setAccommodation(this);
    }
}
