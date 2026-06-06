package com.iyzico.challenge.service;

import com.iyzico.challenge.entity.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class IyzicoPaymentService {

    private Logger logger = LoggerFactory.getLogger(IyzicoPaymentService.class);

    private final BankService bankService;
    private final PaymentDbService paymentDbService;

    public IyzicoPaymentService(BankService bankService, PaymentDbService paymentDbService) {
        this.bankService = bankService;
        this.paymentDbService = paymentDbService;
    }

    public void pay(BigDecimal price) {
        // pay with bank - no active db connection/transaction here
        BankPaymentRequest request = new BankPaymentRequest();
        request.setPrice(price);
        BankPaymentResponse response = bankService.pay(request);

        // insert records - database connection is obtained only for the duration of this save method
        Payment payment = new Payment();
        payment.setBankResponse(response.getResultCode());
        payment.setPrice(price);
        paymentDbService.savePayment(payment);
        logger.info("Payment saved successfully!");
    }
}
