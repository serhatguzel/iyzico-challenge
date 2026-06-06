# iyzico coding challenge

Thank you for applying to work in Iyzico Engineering.

As part of our interview process, we expect you to complete a coding challenge in order for us to understand your coding skills. 
The challenge is a Java11 + Spring Boot project which uses H2 as the database.


# Question 1: Flight Booking System

Most of iyzico's merchants sell products or services online. Flight ticket is one of these services.
For flight booking system the necessary REST services are listed below. We kindly ask you to implement them.

## Requirements

* Flight -> adding, removing and updating
* Seat for existing flight -> adding, removing or updating services. 
* Flight/Seat listing service which returns flight name, description, available seats and price.
* Payment service for the end user to buy their selected seat.
* A seat should not be sold to two passengers.
* If there are 2 passengers pay at the same time for the same seat, first successful should buy the seat and the 2nd one should fail with an appropriate message. We expect IT test for this case.
* No front end is necessary.
* Test coverage for the implemented service should be above 80%. We expect both Integration and unit tests.
* We expect Production Grade solution
* Bonus: Iyzico payment integration can be implemented for payment step. 
Reference: [https://dev.iyzipay.com/](https://dev.iyzipay.com/)


# Question 2 : Latency Management

Iyzico provides its payment service by calling bank endpoints. The bank responses are persisted to database.In [IyzicoPaymentServiceTest.java](src/test/java/com/iyzico/challenge/service/IyzicoPaymentServiceTest.java)
class we have simulated 100 customers calling the payment service.

```java
    public void pay(BigDecimal price) {
        //pay with bank
        BankPaymentRequest request = new BankPaymentRequest();
        request.setPrice(price);
        BankPaymentResponse response = bankService.pay(request);

        //insert records
        Payment payment = new Payment();
        payment.setBankResponse(response.getResultCode());
        payment.setPrice(price);
        paymentRepository.save(payment);
        logger.info("Payment saved successfully!");
    }
```

In the simulation for some reason the bank response times take ~5 seconds. Due to this latency, a database connection problem is encountered after some time. (Running the [IyzicoPaymentServiceTest.java](src/test/java/com/iyzico/challenge/service/IyzicoPaymentServiceTest.java)
class displays "Connection is not available, request timed out after 30005ms." error after some time.)

Find a way to persist bank responses to the database in this situation.

## Requirements

* DB connection pool must stay the same.
* DatabaseConfiguration.java, BankService.java, PaymentServiceClients.java and IyzicoPaymentServiceTest.java classes must not be changed.
* In case of an error, there must not be any inconsistent data in the database.

# Solutions / Çözümler

## ✈️ Question 1 Solution: Flight Booking System

### Architecture & Design
* **Entities:** Developed [Flight](src/main/java/com/iyzico/challenge/entity/Flight.java) and [Seat](src/main/java/com/iyzico/challenge/entity/Seat.java) with structured JPA mappings (`@OneToMany` relationship with cascade configurations).
* **Lombok Integration:** Upgraded project's Lombok version to `1.18.46` to fully support compiling with newer JDKs (such as **JDK 25**), reducing boilerplate code across entities.
* **Infinite Recursion Fix:** Applied `@JsonManagedReference` and `@JsonBackReference` on entities to resolve Jackson's infinite serialization loops.

### Concurrency & Double Booking Prevention
* **Optimistic Locking:** Utilized JPA's `@Version` locking mechanism on the `Seat` entity. When concurrent requests attempt to book the exact same seat, the first transaction commits successfully, incrementing the version. The subsequent transaction fails with an `ObjectOptimisticLockingFailureException`.
* **Exception Handling:** Caught the lock exception globally and mapped it to a clean `409 Conflict` HTTP status (`SeatAlreadySoldException`) providing an appropriate message.
* **Testing:** Written [FlightBookingServiceIT.java](src/test/java/com/iyzico/challenge/FlightBookingServiceIT.java) which simulates two concurrent threads booking the same seat. Only one succeeds and the other fails gracefully.

---

## 💳 Question 2 Solution: Latency Management

### The Root Cause
Originally, the class level `@Transactional` on `IyzicoPaymentService` bound a database connection to the thread for the entire execution of `pay()`. When calling `bankService.pay()`, the thread blocked for **5 seconds** waiting for the bank response while holding the DB connection idle. With 100 concurrent clients, all 10 connections in the Hikari pool were immediately exhausted, causing the remaining 90 threads to time out after 30 seconds.

### The Decoupling Solution
1. Removed `@Transactional` from [IyzicoPaymentService.java](src/main/java/com/iyzico/challenge/service/IyzicoPaymentService.java). Now, the 5-second long bank API call occurs **outside** any database transaction, meaning no connection is held while waiting for the bank.
2. Created a dedicated transactional repository helper: [PaymentDbService.java](src/main/java/com/iyzico/challenge/service/PaymentDbService.java).
3. The database save operation is delegated to `PaymentDbService.savePayment()` which is annotated with `@Transactional(propagation = Propagation.REQUIRES_NEW)`.
4. As a result, the DB connection is only acquired *after* receiving the bank response, held for the duration of the rapid save operation (typically ~1-2 milliseconds), and released immediately back to the pool.

---

## 🧪 Run Tests / Testlerin Çalıştırılması

Run the entire test suite including concurrency and integration tests:
```bash
mvn clean test
```
To run the latency simulation test specifically:
```bash
mvn test -Dtest=IyzicoPaymentServiceTest
```
