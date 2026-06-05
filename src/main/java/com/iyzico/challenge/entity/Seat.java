package com.iyzico.challenge.entity;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Koltuk entity'si.
 *
 * Concurrency güvenliği için @Version anotasyonu kullanılır (Optimistic Lock).
 * İki kullanıcı aynı koltuğu aynı anda almaya çalışırsa, Hibernate
 * ObjectOptimisticLockingFailureException fırlatır.
 */
@Entity
@Table(name = "seats")
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
     * true => Satıldı, false => Müsait
     */
    @Column(nullable = false)
    private boolean isSold = false;

    /**
     * Optimistic Locking için versiyon alanı.
     * Her güncelleme işleminde Hibernate bu değeri otomatik artırır.
     * Eğer iki işlem aynı versiyonu okuyup yazarsa, ikincisi
     * ObjectOptimisticLockingFailureException alır.
     */
    @Version
    private Long version;

    /**
     * Bu koltuğun ait olduğu uçuş.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public boolean isSold() { return isSold; }
    public void setSold(boolean sold) { isSold = sold; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }
}
