package com.iyzico.challenge.exception;

/**
 * Koltuk zaten satılmışsa fırlatılır.
 * HTTP 409 Conflict döndürür.
 */
public class SeatAlreadySoldException extends RuntimeException {
    public SeatAlreadySoldException(String message) {
        super(message);
    }
}
