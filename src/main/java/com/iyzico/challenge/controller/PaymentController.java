package com.iyzico.challenge.controller;

import com.iyzico.challenge.entity.Seat;
import com.iyzico.challenge.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Koltuk satın alma (ödeme) endpoint'i.
 * POST /api/v1/flights/{flightId}/seats/{seatId}/buy
 *
 * Başarı        → 200 OK + Satın alınan koltuk
 * Zaten satıldı → 409 Conflict
 * Eşzamanlılık  → 409 Conflict (Optimistic Lock)
 * Ödeme hatası  → 402 Payment Required
 * Bulunamadı    → 404 Not Found
 */
@RestController
@RequestMapping("/api/v1/flights")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final BookingService bookingService;

    public PaymentController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Belirli bir koltuğu satın alır.
     * POST /api/v1/flights/{flightId}/seats/{seatId}/buy
     */
    @PostMapping("/{flightId}/seats/{seatId}/buy")
    public ResponseEntity<Seat> buySeat(
            @PathVariable Long flightId,
            @PathVariable Long seatId,
            @RequestBody BuySeatRequest request) {
        log.info("POST /api/v1/flights/{}/seats/{}/buy - Satın alma isteği", flightId, seatId);
        Seat seat = bookingService.buySeat(
                flightId, seatId,
                request.getCardHolderName(),
                request.getCardNumber(),
                request.getExpireMonth(),
                request.getExpireYear(),
                request.getCvc()
        );
        return ResponseEntity.ok(seat);
    }

    // ─── İç Request DTO ─────────────────────────────────────────────────────────

    public static class BuySeatRequest {
        private String cardHolderName;
        private String cardNumber;
        private String expireMonth;
        private String expireYear;
        private String cvc;

        public String getCardHolderName() { return cardHolderName; }
        public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }
        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
        public String getExpireMonth() { return expireMonth; }
        public void setExpireMonth(String expireMonth) { this.expireMonth = expireMonth; }
        public String getExpireYear() { return expireYear; }
        public void setExpireYear(String expireYear) { this.expireYear = expireYear; }
        public String getCvc() { return cvc; }
        public void setCvc(String cvc) { this.cvc = cvc; }
    }
}
