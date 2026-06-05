package com.iyzico.challenge.service;

import com.iyzico.challenge.entity.Flight;
import com.iyzico.challenge.entity.Seat;
import com.iyzico.challenge.exception.ResourceNotFoundException;
import com.iyzico.challenge.repository.FlightRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Uçuşlara ait iş mantığını yöneten servis katmanı.
 */
@Service
public class FlightService {

    private static final Logger log = LoggerFactory.getLogger(FlightService.class);

    private final FlightRepository flightRepository;

    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    /**
     * Yeni bir uçuş ekler ve otomatik koltuk üretir.
     *
     * @param name         Uçuş adı
     * @param description  Uçuş açıklaması
     * @param price        Uçuş fiyatı
     * @param numberOfSeats Oluşturulacak koltuk sayısı
     * @return Kaydedilen Flight entity
     */
    @Transactional
    public Flight addFlight(String name, String description, BigDecimal price, int numberOfSeats) {
        log.info("Yeni uçuş ekleniyor: {}", name);

        Flight flight = new Flight();
        flight.setName(name);
        flight.setDescription(description);
        flight.setPrice(price);

        List<Seat> seats = IntStream.rangeClosed(1, numberOfSeats)
                .mapToObj(i -> {
                    Seat seat = new Seat();
                    seat.setSeatNumber(String.format("%02d", i));
                    seat.setPrice(price);
                    seat.setSold(false);
                    seat.setFlight(flight);
                    return seat;
                })
                .collect(Collectors.toList());

        flight.getSeats().addAll(seats);

        Flight saved = flightRepository.save(flight);
        log.info("Uçuş kaydedildi. ID: {}, Koltuk sayısı: {}", saved.getId(), numberOfSeats);
        return saved;
    }

    /**
     * Tüm uçuşları listeler.
     */
    @Transactional(readOnly = true)
    public List<Flight> getAllFlights() {
        return flightRepository.findAll();
    }

    /**
     * ID ile uçuş getirir, bulamazsa ResourceNotFoundException fırlatır.
     */
    @Transactional(readOnly = true)
    public Flight getFlightById(Long flightId) {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Uçuş bulunamadı. ID: " + flightId));
    }

    /**
     * Bir uçuşu günceller.
     */
    @Transactional
    public Flight updateFlight(Long flightId, String name, String description, BigDecimal price) {
        Flight flight = getFlightById(flightId);
        if (name != null) flight.setName(name);
        if (description != null) flight.setDescription(description);
        if (price != null) flight.setPrice(price);
        return flightRepository.save(flight);
    }

    /**
     * Bir uçuşu ve tüm ilişkili koltuklarını siler.
     */
    @Transactional
    public void deleteFlight(Long flightId) {
        Flight flight = getFlightById(flightId);
        flightRepository.delete(flight);
        log.info("Uçuş silindi. ID: {}", flightId);
    }
}
