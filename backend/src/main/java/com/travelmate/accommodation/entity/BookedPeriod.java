package com.travelmate.accommodation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * A date range for an accommodation. {@code startDate} is inclusive (first night),
 * {@code endDate} is exclusive (check-out day), exactly like a hotel booking.
 * Only BOOKED periods block availability.
 */
@Entity
@Table(name = "accommodation_periods", indexes = {
        @Index(name = "idx_period_accommodation_dates", columnList = "accommodation_id,start_date,end_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookedPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PeriodStatus status;

    @Column(length = 255)
    private String note;
}
