package com.iyzico.challenge;

import com.iyzico.challenge.entity.Flight;
import com.iyzico.challenge.entity.Seat;
import com.iyzico.challenge.exception.SeatAlreadySoldException;
import com.iyzico.challenge.repository.FlightRepository;
import com.iyzico.challenge.repository.SeatRepository;
import com.iyzico.challenge.service.BookingService;
import com.iyzico.challenge.service.IyzipayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 *  FlightBookingServiceIT — Eşzamanlılık (Concurrency) Integration Testi
 * ─────────────────────────────────────────────────────────────────────────────
 *
 *  @SpringBootTest: Gerçek Spring context'ini ayağa kaldırır.
 *  H2 in-memory veritabanı kullanılır, dış bağımlılık gerekmez.
 *
 *  SENARYO:
 *    - 2 thread aynı anda aynı koltuğa satın alma isteği gönderir.
 *    - CountDownLatch ile her iki thread de başlamadan önce birbirini bekler.
 *
 *  BEKLENEN SONUÇ:
 *    - Thread'lerden tam olarak 1'i başarılı olur.
 *    - Diğeri SeatAlreadySoldException veya ObjectOptimisticLockingFailureException alır.
 *    - Veritabanında koltuk sadece 1 kez satılmış görünür.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@SpringBootTest
class FlightBookingServiceIT {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private SeatRepository seatRepository;

    @MockBean
    private IyzipayService iyzipayService;

    private Long flightId;
    private Long seatId;

    private static final String CARD_HOLDER = "John Doe";
    private static final String CARD_NUMBER = "5526080000000006";
    private static final String EXPIRE_MONTH = "12";
    private static final String EXPIRE_YEAR = "2030";
    private static final String CVC = "123";

    @BeforeEach
    void setUp() {
        seatRepository.deleteAll();
        flightRepository.deleteAll();

        Flight flight = new Flight();
        flight.setName("Test Uçuşu");
        flight.setDescription("Concurrency test için");
        flight.setPrice(BigDecimal.valueOf(1000));

        Seat seat = new Seat();
        seat.setSeatNumber("01");
        seat.setPrice(BigDecimal.valueOf(1000));
        seat.setSold(false);
        seat.setFlight(flight);

        flight.getSeats().add(seat);
        Flight savedFlight = flightRepository.save(flight);

        this.flightId = savedFlight.getId();
        this.seatId = savedFlight.getSeats().get(0).getId();
    }

    /**
     * ANA TEST: 2 thread aynı anda aynı koltuğu satın almaya çalışır.
     * Sadece 1'i başarılı olmalı, diğeri hata almalı.
     */
    @Test
    @DisplayName("Aynı koltuğa eş zamanlı 2 istek geldiğinde sadece 1'i başarılı olmalı")
    void whenTwoUsersBookSameSeat_thenOnlyOneSucceeds() throws InterruptedException {
        int threadCount = 2;

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Throwable> caughtExceptions = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    doNothing().when(iyzipayService).pay(any(), any(), any(), any(), any(), any(), any());
                    bookingService.buySeat(flightId, seatId,
                            CARD_HOLDER, CARD_NUMBER, EXPIRE_MONTH, EXPIRE_YEAR, CVC);
                    successCount.incrementAndGet();
                } catch (SeatAlreadySoldException | ObjectOptimisticLockingFailureException e) {
                    failureCount.incrementAndGet();
                    caughtExceptions.add(e);
                } catch (Exception e) {
                    caughtExceptions.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed)
                .as("Her iki thread de 10 saniye içinde tamamlanmalıydı")
                .isTrue();

        assertThat(successCount.get() + failureCount.get())
                .as("Tüm thread'ler bir sonuç üretmeli")
                .isEqualTo(threadCount);

        assertThat(successCount.get())
                .as("Sadece 1 thread başarılı olmalı")
                .isEqualTo(1);

        assertThat(failureCount.get())
                .as("1 thread hata almalı")
                .isEqualTo(1);

        Seat seatInDb = seatRepository.findById(seatId).orElseThrow();
        assertThat(seatInDb.isSold())
                .as("Koltuk veritabanında satılmış görünmeli")
                .isTrue();

        long soldSeatCount = seatRepository.findAll().stream()
                .filter(Seat::isSold)
                .count();
        assertThat(soldSeatCount)
                .as("Veritabanında tam olarak 1 koltuk satılmış olmalı (double-booking yok)")
                .isEqualTo(1);
    }

    /**
     * EK TEST: Farklı koltuklar için eş zamanlı işlemler birbirini engellememeli.
     */
    @Test
    @DisplayName("Farklı koltuklar için eş zamanlı satın alma işlemleri birbirini engellemez")
    void whenTwoUsersBookDifferentSeats_thenBothSucceed() throws InterruptedException {
        Flight flight = flightRepository.findById(flightId).orElseThrow();

        Seat secondSeat = new Seat();
        secondSeat.setSeatNumber("02");
        secondSeat.setPrice(BigDecimal.valueOf(1000));
        secondSeat.setSold(false);
        secondSeat.setFlight(flight);
        Seat savedSecondSeat = seatRepository.save(secondSeat);
        Long secondSeatId = savedSecondSeat.getId();

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            try {
                startGate.await();
                doNothing().when(iyzipayService).pay(any(), any(), any(), any(), any(), any(), any());
                bookingService.buySeat(flightId, seatId,
                        CARD_HOLDER, CARD_NUMBER, EXPIRE_MONTH, EXPIRE_YEAR, CVC);
                successCount.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startGate.await();
                doNothing().when(iyzipayService).pay(any(), any(), any(), any(), any(), any(), any());
                bookingService.buySeat(flightId, secondSeatId,
                        CARD_HOLDER, CARD_NUMBER, EXPIRE_MONTH, EXPIRE_YEAR, CVC);
                successCount.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        startGate.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get())
                .as("Farklı koltuklar için her iki thread de başarılı olmalı")
                .isEqualTo(2);

        assertThat(seatRepository.findById(seatId).orElseThrow().isSold()).isTrue();
        assertThat(seatRepository.findById(secondSeatId).orElseThrow().isSold()).isTrue();
    }
}
