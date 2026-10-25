package com.travelmate.accommodation.repository;

import com.travelmate.accommodation.entity.BookedPeriod;
import com.travelmate.accommodation.entity.PeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookedPeriodRepository extends JpaRepository<BookedPeriod, Long> {

    List<BookedPeriod> findByAccommodationIdOrderByStartDateAsc(Long accommodationId);

    Optional<BookedPeriod> findByIdAndAccommodationId(Long id, Long accommodationId);

    /** Periods with the given status that overlap [from, to) (end dates are exclusive). */
    @Query("""
            select p from BookedPeriod p
            where p.accommodation.id = :accommodationId
              and p.status = :status
              and p.startDate < :to
              and p.endDate > :from
            order by p.startDate
            """)
    List<BookedPeriod> findOverlapping(@Param("accommodationId") Long accommodationId,
                                       @Param("status") PeriodStatus status,
                                       @Param("from") LocalDate from,
                                       @Param("to") LocalDate to);
}
