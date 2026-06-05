package com.iyzico.challenge.repository;

import com.iyzico.challenge.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Seat entity'si için Spring Data JPA repository'si.
 */
@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    /**
     * Belirli bir uçuştaki müsait (satılmamış) koltukları getirir.
     */
    @Query("SELECT s FROM Seat s WHERE s.flight.id = :flightId AND s.isSold = false")
    List<Seat> findAvailableSeatsByFlightId(@Param("flightId") Long flightId);

    /**
     * Belirli bir uçuşa ait, belirli ID'ye sahip koltuğu getirir.
     * Satın alma işleminde güvenli arama için kullanılır.
     */
    Optional<Seat> findByIdAndFlightId(Long seatId, Long flightId);
}
