package com.iyzico.challenge.service;

import com.iyzico.challenge.entity.Flight;
import com.iyzico.challenge.entity.Seat;
import com.iyzico.challenge.exception.ResourceNotFoundException;
import com.iyzico.challenge.exception.SeatAlreadySoldException;
import com.iyzico.challenge.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Unit Testleri")
class BookingServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private IyzipayService iyzipayService;

    @InjectMocks
    private BookingService bookingService;

    private Flight sampleFlight;
    private Seat availableSeat;
    private Seat soldSeat;

    // Kart bilgileri
    private static final String CARD_HOLDER = "John Doe";
    private static final String CARD_NUMBER = "5526080000000006";
    private static final String EXPIRE_MONTH = "12";
    private static final String EXPIRE_YEAR = "2030";
    private static final String CVC = "123";

    @BeforeEach
    void setUp() {
        sampleFlight = new Flight();
        sampleFlight.setId(1L);
        sampleFlight.setName("Istanbul - Ankara");
        sampleFlight.setPrice(BigDecimal.valueOf(1500));

        availableSeat = new Seat();
        availableSeat.setId(10L);
        availableSeat.setSeatNumber("01");
        availableSeat.setPrice(BigDecimal.valueOf(1500));
        availableSeat.setSold(false);
        availableSeat.setVersion(0L);
        availableSeat.setFlight(sampleFlight);

        soldSeat = new Seat();
        soldSeat.setId(11L);
        soldSeat.setSeatNumber("02");
        soldSeat.setPrice(BigDecimal.valueOf(1500));
        soldSeat.setSold(true);
        soldSeat.setVersion(1L);
        soldSeat.setFlight(sampleFlight);
    }

    @Nested
    @DisplayName("buySeat()")
    class BuySeat {

        @Test
        @DisplayName("Müsait koltuk başarıyla satılır ve isSold=true olur")
        void givenAvailableSeat_whenBuySeat_thenSeatIsSold() {
            when(seatRepository.findByIdAndFlightId(10L, 1L))
                    .thenReturn(Optional.of(availableSeat));
            doNothing().when(iyzipayService).pay(any(), any(), any(), any(), any(), any(), any());
            when(seatRepository.save(any(Seat.class))).thenReturn(availableSeat);

            Seat result = bookingService.buySeat(1L, 10L, CARD_HOLDER, CARD_NUMBER,
                    EXPIRE_MONTH, EXPIRE_YEAR, CVC);

            assertThat(result).isNotNull();
            assertThat(result.getSeatNumber()).isEqualTo("01");
            assertThat(result.isSold()).isTrue();
            verify(seatRepository, times(1)).save(availableSeat);
        }

        @Test
        @DisplayName("save() çağrılmadan önce isSold=true yapılır")
        void givenAvailableSeat_whenBuySeat_thenSoldFlagSetBeforeSave() {
            when(seatRepository.findByIdAndFlightId(10L, 1L))
                    .thenReturn(Optional.of(availableSeat));
            doNothing().when(iyzipayService).pay(any(), any(), any(), any(), any(), any(), any());
            when(seatRepository.save(any(Seat.class))).thenAnswer(inv -> {
                Seat s = inv.getArgument(0);
                assertThat(s.isSold()).isTrue();
                return s;
            });

            bookingService.buySeat(1L, 10L, CARD_HOLDER, CARD_NUMBER, EXPIRE_MONTH, EXPIRE_YEAR, CVC);
        }

        @Test
        @DisplayName("Koltuk zaten satılmışsa SeatAlreadySoldException fırlatır")
        void givenSoldSeat_whenBuySeat_thenThrowSeatAlreadySoldException() {
            when(seatRepository.findByIdAndFlightId(11L, 1L))
                    .thenReturn(Optional.of(soldSeat));

            assertThatThrownBy(() -> bookingService.buySeat(1L, 11L, CARD_HOLDER, CARD_NUMBER,
                    EXPIRE_MONTH, EXPIRE_YEAR, CVC))
                    .isInstanceOf(SeatAlreadySoldException.class)
                    .hasMessageContaining("02");

            verify(seatRepository, never()).save(any());
        }

        @Test
        @DisplayName("Koltuk bulunamazsa ResourceNotFoundException fırlatır")
        void givenSeatNotFound_whenBuySeat_thenThrowResourceNotFoundException() {
            when(seatRepository.findByIdAndFlightId(99L, 1L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.buySeat(1L, 99L, CARD_HOLDER, CARD_NUMBER,
                    EXPIRE_MONTH, EXPIRE_YEAR, CVC))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99")
                    .hasMessageContaining("1");

            verify(seatRepository, never()).save(any());
        }

        @Test
        @DisplayName("Yanlış uçuş ID'siyle koltuk bulunamazsa ResourceNotFoundException fırlatır")
        void givenWrongFlightId_whenBuySeat_thenThrowResourceNotFoundException() {
            when(seatRepository.findByIdAndFlightId(10L, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.buySeat(999L, 10L, CARD_HOLDER, CARD_NUMBER,
                    EXPIRE_MONTH, EXPIRE_YEAR, CVC))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
