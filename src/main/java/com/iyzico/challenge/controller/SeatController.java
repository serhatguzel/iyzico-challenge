package com.iyzico.challenge.controller;

import com.iyzico.challenge.entity.Seat;
import com.iyzico.challenge.service.SeatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Koltuk CRUD işlemleri için REST API endpoint'leri.
 * GET /api/v1/flights/{flightId}/seats         → Müsait koltukları listele
 * POST /api/v1/flights/{flightId}/seats        → Koltuğa koltuk ekle
 * PUT /api/v1/flights/{flightId}/seats/{id}    → Koltuğu güncelle
 * DELETE /api/v1/flights/{flightId}/seats/{id} → Koltuğu sil
 */
@RestController
@RequestMapping("/api/v1/flights/{flightId}/seats")
public class SeatController {

    private static final Logger log = LoggerFactory.getLogger(SeatController.class);

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    /**
     * Belirli bir uçuşun müsait (satılmamış) koltukları.
     * GET /api/v1/flights/{flightId}/seats
     */
    @GetMapping
    public ResponseEntity<List<Seat>> getAvailableSeats(@PathVariable Long flightId) {
        log.info("GET /api/v1/flights/{}/seats - Müsait koltuklar listeleniyor", flightId);
        return ResponseEntity.ok(seatService.getAvailableSeats(flightId));
    }

    /**
     * Bir uçuşa yeni koltuk ekler.
     * POST /api/v1/flights/{flightId}/seats
     */
    @PostMapping
    public ResponseEntity<Seat> addSeat(@PathVariable Long flightId,
                                        @RequestBody SeatRequest request) {
        log.info("POST /api/v1/flights/{}/seats - Koltuk ekleme isteği", flightId);
        Seat seat = seatService.addSeat(flightId, request.getSeatNumber(), request.getPrice());
        return ResponseEntity.status(HttpStatus.CREATED).body(seat);
    }

    /**
     * Bir koltuğu günceller.
     * PUT /api/v1/flights/{flightId}/seats/{seatId}
     */
    @PutMapping("/{seatId}")
    public ResponseEntity<Seat> updateSeat(@PathVariable Long flightId,
                                           @PathVariable Long seatId,
                                           @RequestBody SeatRequest request) {
        log.info("PUT /api/v1/flights/{}/seats/{} - Koltuk güncelleme isteği", flightId, seatId);
        Seat updated = seatService.updateSeat(seatId, request.getSeatNumber(), request.getPrice());
        return ResponseEntity.ok(updated);
    }

    /**
     * Bir koltuğu siler.
     * DELETE /api/v1/flights/{flightId}/seats/{seatId}
     */
    @DeleteMapping("/{seatId}")
    public ResponseEntity<Void> deleteSeat(@PathVariable Long flightId,
                                           @PathVariable Long seatId) {
        log.info("DELETE /api/v1/flights/{}/seats/{} - Koltuk silme isteği", flightId, seatId);
        seatService.deleteSeat(seatId);
        return ResponseEntity.noContent().build();
    }

    // ─── İç Request DTO ─────────────────────────────────────────────────────────

    public static class SeatRequest {
        private String seatNumber;
        private BigDecimal price;

        public String getSeatNumber() { return seatNumber; }
        public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }
}
