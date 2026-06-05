package com.iyzico.challenge.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller'larda oluşabilecek tüm hataları merkezi olarak yöneten sınıf.
 * Stack trace sızmasını önler ve tutarlı HTTP durum kodları döndürür.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bulunamayan uçuş veya koltuk → HTTP 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Kaynak bulunamadı: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Koltuk zaten satılmışsa → HTTP 409
     */
    @ExceptionHandler(SeatAlreadySoldException.class)
    public ResponseEntity<Map<String, Object>> handleSeatAlreadySoldException(
            SeatAlreadySoldException ex, HttpServletRequest request) {
        log.warn("Satış hatası (Zaten satılmış): {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Optimistic Locking çakışması (eş zamanlı satın alma) → HTTP 409
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLockException(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        log.error("Eşzamanlılık çakışması (Optimistic Lock): {}", ex.getMessage());
        String message = "Koltuk başka bir işleme konu oldu. Lütfen tekrar deneyiniz.";
        return buildErrorResponse(HttpStatus.CONFLICT, message, request.getRequestURI());
    }

    /**
     * Iyzico ödemesi başarısız → HTTP 402
     */
    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentFailedException(
            PaymentFailedException ex, HttpServletRequest request) {
        log.warn("Ödeme hatası: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.PAYMENT_REQUIRED, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Validation hataları (@NotBlank, @NotNull vb.) → HTTP 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" | "));
        log.warn("Doğrulama hatası: {}", errors);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, errors, request.getRequestURI());
    }

    /**
     * Beklenmeyen tüm hatalar → HTTP 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Beklenmeyen bir hata oluştu: ", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Sistemsel bir hata oluştu, lütfen daha sonra tekrar deneyiniz.",
                request.getRequestURI());
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return new ResponseEntity<>(body, status);
    }
}
