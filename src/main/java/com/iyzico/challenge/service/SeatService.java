package com.iyzico.challenge.service;

import com.iyzico.challenge.entity.Seat;
import com.iyzico.challenge.exception.ResourceNotFoundException;
import com.iyzico.challenge.repository.FlightRepository;
import com.iyzico.challenge.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Koltuk CRUD işlemlerini yöneten servis.
 */
@Service
public class SeatService {

    private static final Logger log = LoggerFactory.getLogger(SeatService.class);

    private final SeatRepository seatRepository;
    private final FlightRepository flightRepository;

    public SeatService(SeatRepository seatRepository, FlightRepository flightRepository) {
        this.seatRepository = seatRepository;
        this.flightRepository = flightRepository;
    }

    /**
     * Belirli bir uçuşun müsait koltukları.
     */
    @Transactional(readOnly = true)
    public List<Seat> getAvailableSeats(Long flightId) {
        if (!flightRepository.existsById(flightId)) {
            throw new ResourceNotFoundException("Uçuş bulunamadı. ID: " + flightId);
        }
        return seatRepository.findAvailableSeatsByFlightId(flightId);
    }

    /**
     * Bir uçuşa manuel olarak koltuk ekler.
     */
    @Transactional
    public Seat addSeat(Long flightId, String seatNumber, BigDecimal price) {
        return flightRepository.findById(flightId).map(flight -> {
            Seat seat = new Seat();
            seat.setSeatNumber(seatNumber);
            seat.setPrice(price);
            seat.setSold(false);
            seat.setFlight(flight);
            Seat saved = seatRepository.save(seat);
            log.info("Koltuk eklendi. No: {}, Uçuş ID: {}", seatNumber, flightId);
            return saved;
        }).orElseThrow(() -> new ResourceNotFoundException("Uçuş bulunamadı. ID: " + flightId));
    }

    /**
     * Bir koltuğu günceller.
     */
    @Transactional
    public Seat updateSeat(Long seatId, String seatNumber, BigDecimal price) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Koltuk bulunamadı. ID: " + seatId));
        if (seatNumber != null) seat.setSeatNumber(seatNumber);
        if (price != null) seat.setPrice(price);
        return seatRepository.save(seat);
    }

    /**
     * Bir koltuğu siler.
     */
    @Transactional
    public void deleteSeat(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Koltuk bulunamadı. ID: " + seatId));
        seatRepository.delete(seat);
        log.info("Koltuk silindi. ID: {}", seatId);
    }
}
