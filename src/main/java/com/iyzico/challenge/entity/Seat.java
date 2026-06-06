package com.iyzico.challenge.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Koltuk entity'si.
 *
 * Concurrency güvenliği için @Version anotasyonu kullanılır (Optimistic Lock).
 * İki kullanıcı aynı koltuğu aynı anda almaya çalışırsa, Hibernate
 * ObjectOptimisticLockingFailureException fırlatır.
 *
 * @JsonBackReference: Seat→Flight→Seat sonsuz JSON döngüsünü kırar;
 *                     flight alanı JSON serialize edilmez.
 */
@Entity
@Table(name = "seats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String seatNumber;

    /**
     * Koltuğun fiyatı.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    /**
     * Koltuğun satılıp satılmadığını belirtir.
     * true = Satıldı, false = Müsait
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean isSold = false;

    /**
     * Optimistic Locking için versiyon alanı.
     * Her güncelleme işleminde Hibernate bu değeri otomatik artırır.
     * Çakışma olursa ObjectOptimisticLockingFailureException fırlatılır.
     */
    @Version
    private Long version;

    /**
     * Bu koltuğun ait olduğu uçuş.
     * @JsonBackReference ile JSON serialize edilmez (döngü önlenir).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    @JsonBackReference
    private Flight flight;
}
