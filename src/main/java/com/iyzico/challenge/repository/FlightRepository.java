package com.iyzico.challenge.repository;

import com.iyzico.challenge.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Flight entity'si için Spring Data JPA repository'si.
 */
@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {
}
