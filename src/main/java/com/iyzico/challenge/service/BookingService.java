package com.iyzico.challenge.service;

import com.iyzico.challenge.entity.Seat;
import com.iyzico.challenge.exception.ResourceNotFoundException;
import com.iyzico.challenge.exception.SeatAlreadySoldException;
import com.iyzico.challenge.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Koltuk satın alma iş mantığını yöneten kritik servis katmanı.
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │              CONCURRENCY SENARYOSU                      │
 * │                                                         │
 * │  Thread A ──► findByIdAndFlightId ──► isSold=false      │
 * │  Thread B ──► findByIdAndFlightId ──► isSold=false      │
 * │                                                         │
 * │  Thread A ──► setSold(true) ──► save (version: 0→1) ✅  │
 * │  Thread B ──► setSold(true) ──► save (version: 0, ama   │
 * │               DB'de 1!) ──► ObjectOptimisticLocking      │
 * │               FailureException ❌                        │
 * └─────────────────────────────────────────────────────────┘
 *
 * GlobalExceptionHandler bu hatayı yakalar ve HTTP 409 döner.
 * @Transactional sayesinde hata durumunda DB'de tutarsız veri kalmaz.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final SeatRepository seatRepository;
    private final IyzipayService iyzipayService;

    public BookingService(SeatRepository seatRepository, IyzipayService iyzipayService) {
        this.seatRepository = seatRepository;
        this.iyzipayService = iyzipayService;
    }

    /**
     * Koltuk satın alma işleminin tek noktası.
     *
     * Bu metot @Transactional ile işaretlenmiştir:
     * - Koltuk okunur, isSold kontrolü yapılır ve güncelleme tek bir atomik işlem içinde gerçekleşir.
     * - İşlem başarısız olursa tüm değişiklikler geri alınır (rollback).
     * - Optimistic lock çakışmasında Hibernate ObjectOptimisticLockingFailureException fırlatır.
     *
     * @param flightId    Uçuş ID'si
     * @param seatId      Koltuk ID'si
     * @param cardHolderName Kart üzerindeki isim
     * @param cardNumber  Kart numarası
     * @param expireMonth Son kullanma ayı
     * @param expireYear  Son kullanma yılı
     * @param cvc         CVC kodu
     * @return Satın alınan Seat entity
     */
    @Transactional
    public Seat buySeat(Long flightId, Long seatId,
                        String cardHolderName, String cardNumber,
                        String expireMonth, String expireYear, String cvc) {
        log.info("Koltuk satın alma isteği alındı. Uçuş ID: {}, Koltuk ID: {}", flightId, seatId);

        // 1. Koltuğu bul (hem seatId hem flightId eşleşmeli)
        Seat seat = seatRepository.findByIdAndFlightId(seatId, flightId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Koltuk bulunamadı. Koltuk ID: %d, Uçuş ID: %d", seatId, flightId)));

        // 2. Koltuk zaten satılmış mı kontrol et
        if (seat.isSold()) {
            throw new SeatAlreadySoldException(
                    String.format("Koltuk zaten satılmış. Koltuk No: %s", seat.getSeatNumber()));
        }

        // 3. Iyzico Ödeme Adımı (Lazy yüklendiği için flight, burada DB'den çekilir)
        iyzipayService.pay(seat.getFlight(), seat, cardHolderName, cardNumber, expireMonth, expireYear, cvc);

        // 4. Koltuğu sat (version alanı Hibernate tarafından otomatik artırılır)
        seat.setSold(true);
        Seat savedSeat = seatRepository.save(seat);

        log.info("Koltuk başarıyla satıldı. Koltuk No: {}, Uçuş ID: {}", seat.getSeatNumber(), flightId);
        return savedSeat;
    }
}
