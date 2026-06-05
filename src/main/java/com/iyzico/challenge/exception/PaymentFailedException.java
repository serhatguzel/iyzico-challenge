package com.iyzico.challenge.exception;

/**
 * Iyzico ödemesi başarısız olduğunda fırlatılır.
 * HTTP 402 Payment Required döndürür.
 */
public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message) {
        super(message);
    }
}
