package com.iyzico.challenge.service;

import com.iyzico.challenge.entity.Flight;
import com.iyzico.challenge.entity.Seat;
import com.iyzico.challenge.exception.ResourceNotFoundException;
import com.iyzico.challenge.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlightService Unit Testleri")
class FlightServiceTest {

    @Mock
    private FlightRepository flightRepository;

    @InjectMocks
    private FlightService flightService;

    private Flight sampleFlight;

    @BeforeEach
    void setUp() {
        sampleFlight = new Flight();
        sampleFlight.setId(1L);
        sampleFlight.setName("Istanbul - Ankara");
        sampleFlight.setDescription("Sabah uçuşu");
        sampleFlight.setPrice(BigDecimal.valueOf(1500));
    }

    // ─── addFlight ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addFlight()")
    class AddFlight {

        @Test
        @DisplayName("Geçerli istek ile uçuş başarıyla oluşturulur")
        void givenValidRequest_whenAddFlight_thenReturnFlight() {
            when(flightRepository.save(any(Flight.class))).thenReturn(sampleFlight);

            Flight result = flightService.addFlight("Istanbul - Ankara", "Sabah uçuşu",
                    BigDecimal.valueOf(1500), 3);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Istanbul - Ankara");
            assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(1500));
            verify(flightRepository, times(1)).save(any(Flight.class));
        }

        @Test
        @DisplayName("numberOfSeats kadar koltuk oluşturulur")
        void givenRequest_whenAddFlight_thenSeatsAreGenerated() {
            when(flightRepository.save(any(Flight.class))).thenAnswer(inv -> {
                Flight f = inv.getArgument(0);
                f.setId(1L);
                return f;
            });

            flightService.addFlight("Istanbul - Ankara", "Sabah uçuşu",
                    BigDecimal.valueOf(1500), 3);

            verify(flightRepository, times(1)).save(argThat(flight ->
                    flight.getSeats().size() == 3
            ));
        }

        @Test
        @DisplayName("Koltuk numaraları sıfırla doldurulmuş formatta üretilir (01, 02...)")
        void givenRequest_whenAddFlight_thenSeatNumbersAreFormatted() {
            when(flightRepository.save(any(Flight.class))).thenAnswer(inv -> {
                Flight f = inv.getArgument(0);
                f.setId(1L);
                return f;
            });

            flightService.addFlight("Istanbul - Ankara", "Sabah uçuşu",
                    BigDecimal.valueOf(1500), 3);

            verify(flightRepository).save(argThat(flight ->
                    flight.getSeats().stream()
                            .map(Seat::getSeatNumber)
                            .allMatch(num -> num.matches("\\d{2}"))
            ));
        }
    }

    // ─── getAllFlights ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllFlights()")
    class GetAllFlights {

        @Test
        @DisplayName("Uçuşlar varsa listeyi döner")
        void givenFlightsExist_whenGetAllFlights_thenReturnList() {
            Flight flight2 = new Flight();
            flight2.setId(2L);
            flight2.setName("Ankara - Izmir");
            flight2.setPrice(BigDecimal.valueOf(800));

            when(flightRepository.findAll()).thenReturn(List.of(sampleFlight, flight2));

            List<Flight> result = flightService.getAllFlights();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Flight::getName)
                    .containsExactly("Istanbul - Ankara", "Ankara - Izmir");
        }

        @Test
        @DisplayName("Hiç uçuş yoksa boş liste döner")
        void givenNoFlights_whenGetAllFlights_thenReturnEmptyList() {
            when(flightRepository.findAll()).thenReturn(List.of());

            List<Flight> result = flightService.getAllFlights();

            assertThat(result).isEmpty();
        }
    }

    // ─── deleteFlight ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteFlight()")
    class DeleteFlight {

        @Test
        @DisplayName("Uçuş varsa başarıyla silinir")
        void givenFlightExists_whenDeleteFlight_thenDeleteCalled() {
            when(flightRepository.findById(1L)).thenReturn(Optional.of(sampleFlight));

            flightService.deleteFlight(1L);

            verify(flightRepository, times(1)).delete(sampleFlight);
        }

        @Test
        @DisplayName("Uçuş bulunamazsa ResourceNotFoundException fırlatır")
        void givenFlightNotFound_whenDeleteFlight_thenThrowException() {
            when(flightRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> flightService.deleteFlight(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            verify(flightRepository, never()).delete(any());
        }
    }
}
