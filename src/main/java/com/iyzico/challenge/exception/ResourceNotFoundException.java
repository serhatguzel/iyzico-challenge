package com.iyzico.challenge.exception;

/**
 * İstenen kaynak (uçuş veya koltuk) bulunamadığında fırlatılır.
 * HTTP 404 Not Found döndürür.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
