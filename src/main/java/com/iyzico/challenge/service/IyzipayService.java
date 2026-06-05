package com.iyzico.challenge.service;

import com.iyzico.challenge.entity.Flight;
import com.iyzico.challenge.entity.Seat;
import com.iyzico.challenge.exception.PaymentFailedException;
import com.iyzipay.Options;
import com.iyzipay.model.*;
import com.iyzipay.request.CreatePaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Iyzico üzerinden gerçek ödeme alan servis.
 * PaymentService adıyla çakışmaması için IyzipayService adıyla oluşturuldu.
 */
@Service
public class IyzipayService {

    private static final Logger log = LoggerFactory.getLogger(IyzipayService.class);

    @Value("${iyzipay.api-key:sandbox-key}")
    private String apiKey;

    @Value("${iyzipay.secret-key:sandbox-secret}")
    private String secretKey;

    @Value("${iyzipay.base-url:https://sandbox-api.iyzipay.com}")
    private String baseUrl;

    /**
     * Iyzico üzerinden ödeme alma işlemini başlatır.
     *
     * @param flight      Uçuş bilgisi
     * @param seat        Koltuk bilgisi
     * @param cardHolderName Kart üzerindeki isim
     * @param cardNumber  Kart numarası
     * @param expireMonth Son kullanma ayı
     * @param expireYear  Son kullanma yılı
     * @param cvc         CVC kodu
     * @throws PaymentFailedException Ödeme başarısız olursa fırlatılır
     */
    public void pay(Flight flight, Seat seat,
                    String cardHolderName, String cardNumber,
                    String expireMonth, String expireYear, String cvc) {
        log.info("Iyzico ödeme isteği hazırlanıyor. Koltuk ID: {}, Fiyat: {}", seat.getId(), seat.getPrice());

        Options options = new Options();
        options.setApiKey(apiKey);
        options.setSecretKey(secretKey);
        options.setBaseUrl(baseUrl);

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setLocale(Locale.TR.getValue());
        request.setConversationId(UUID.randomUUID().toString());
        request.setPrice(seat.getPrice());
        request.setPaidPrice(seat.getPrice());
        request.setCurrency(Currency.TRY.name());
        request.setInstallment(1);
        request.setPaymentChannel(PaymentChannel.WEB.name());
        request.setPaymentGroup(PaymentGroup.PRODUCT.name());

        PaymentCard paymentCard = new PaymentCard();
        paymentCard.setCardHolderName(cardHolderName);
        paymentCard.setCardNumber(cardNumber);
        paymentCard.setExpireMonth(expireMonth);
        paymentCard.setExpireYear(expireYear);
        paymentCard.setCvc(cvc);
        paymentCard.setRegisterCard(0);
        request.setPaymentCard(paymentCard);

        Buyer buyer = new Buyer();
        buyer.setId("BY789");
        buyer.setName("John");
        buyer.setSurname("Doe");
        buyer.setGsmNumber("+905350000000");
        buyer.setEmail("email@email.com");
        buyer.setIdentityNumber("74300864791");
        buyer.setRegistrationAddress("Nidakule Göztepe, Merdivenköy Mah. Bora Sok. No:1");
        buyer.setIp("85.34.78.112");
        buyer.setCity("Istanbul");
        buyer.setCountry("Turkey");
        request.setBuyer(buyer);

        Address address = new Address();
        address.setContactName("John Doe");
        address.setCity("Istanbul");
        address.setCountry("Turkey");
        address.setAddress("Nidakule Göztepe, Merdivenköy Mah. Bora Sok. No:1");
        request.setShippingAddress(address);
        request.setBillingAddress(address);

        List<BasketItem> basketItems = new ArrayList<>();
        BasketItem basketItem = new BasketItem();
        basketItem.setId("SEAT-" + seat.getId());
        basketItem.setName(flight.getName() + " - Seat " + seat.getSeatNumber());
        basketItem.setCategory1("Flight Ticket");
        basketItem.setItemType(BasketItemType.VIRTUAL.name());
        basketItem.setPrice(seat.getPrice());
        basketItems.add(basketItem);
        request.setBasketItems(basketItems);

        com.iyzipay.model.Payment payment = com.iyzipay.model.Payment.create(request, options);

        if (!"success".equalsIgnoreCase(payment.getStatus())) {
            log.error("Iyzico ödemesi başarısız! Hata Kodu: {}, Hata Mesajı: {}",
                    payment.getErrorCode(), payment.getErrorMessage());
            throw new PaymentFailedException("Ödeme alınamadı: " + payment.getErrorMessage());
        }

        log.info("Iyzico ödemesi başarıyla tamamlandı. PaymentId: {}", payment.getPaymentId());
    }
}
