package com.iyzico.challenge.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Uçuş entity'si.
 * Bir uçuşun temel bilgilerini ve ona ait koltukları tutar.
 */
@Entity
@Table(name = "flights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flight {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * Uçuşun baz fiyatı. Para birimleri için BigDecimal zorunludur.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    /**
     * Bu uçuşa ait koltuklar. Cascade ile birlikte kayıt/silme işlemi yapılır.
     * @JsonManagedReference: Flight→Seat→Flight sonsuz JSON döngüsünü kırar.
     */
    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    private List<Seat> seats = new ArrayList<>();
}
