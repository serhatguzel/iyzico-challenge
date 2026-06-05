package com.iyzico.challenge.controller;

import com.iyzico.challenge.entity.Flight;
import com.iyzico.challenge.service.FlightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Uçuş CRUD işlemleri için REST API endpoint'leri.
 * GET /api/v1/flights            → Tüm uçuşları listele
 * POST /api/v1/flights           → Yeni uçuş ekle
 * PUT /api/v1/flights/{id}       → Uçuş güncelle
 * DELETE /api/v1/flights/{id}    → Uçuş sil
 */
@RestController
@RequestMapping("/api/v1/flights")
public class FlightController {

    private static final Logger log = LoggerFactory.getLogger(FlightController.class);

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    /**
     * Tüm uçuşları listeler.
     * GET /api/v1/flights
     */
    @GetMapping
    public ResponseEntity<List<Flight>> getAllFlights() {
        log.info("GET /api/v1/flights - Tüm uçuşlar listeleniyor");
        return ResponseEntity.ok(flightService.getAllFlights());
    }

    /**
     * Yeni bir uçuş ekler.
     * POST /api/v1/flights
     */
    @PostMapping
    public ResponseEntity<Flight> addFlight(@RequestBody FlightRequest request) {
        log.info("POST /api/v1/flights - Uçuş ekleme isteği: {}", request.getName());
        Flight flight = flightService.addFlight(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getNumberOfSeats() > 0 ? request.getNumberOfSeats() : 10
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(flight);
    }

    /**
     * Bir uçuşu günceller.
     * PUT /api/v1/flights/{flightId}
     */
    @PutMapping("/{flightId}")
    public ResponseEntity<Flight> updateFlight(@PathVariable Long flightId,
                                               @RequestBody FlightRequest request) {
        log.info("PUT /api/v1/flights/{} - Uçuş güncelleme isteği", flightId);
        Flight updated = flightService.updateFlight(
                flightId, request.getName(), request.getDescription(), request.getPrice());
        return ResponseEntity.ok(updated);
    }

    /**
     * Bir uçuşu siler.
     * DELETE /api/v1/flights/{flightId}
     */
    @DeleteMapping("/{flightId}")
    public ResponseEntity<Void> deleteFlight(@PathVariable Long flightId) {
        log.info("DELETE /api/v1/flights/{} - Uçuş silme isteği", flightId);
        flightService.deleteFlight(flightId);
        return ResponseEntity.noContent().build();
    }

    // ─── İç Request DTO ─────────────────────────────────────────────────────────

    public static class FlightRequest {
        private String name;
        private String description;
        private BigDecimal price;
        private int numberOfSeats = 10;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public int getNumberOfSeats() { return numberOfSeats; }
        public void setNumberOfSeats(int numberOfSeats) { this.numberOfSeats = numberOfSeats; }
    }
}
