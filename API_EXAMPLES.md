# Iyzico Flight Payment - API Örnek İstekleri

## 1. Uçuş Ekle

```bash
curl -X POST http://localhost:8080/api/v1/flights \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Istanbul - Ankara",
    "description": "Sabah uçuşu 07:00",
    "price": 1500.00,
    "numberOfSeats": 5
  }'
```

**Yanıt:**
```json
{
  "id": 1,
  "name": "Istanbul - Ankara",
  "description": "Sabah uçuşu 07:00",
  "price": 1500.00
}
```

---

## 2. Tüm Uçuşları Listele

```bash
curl http://localhost:8080/api/v1/flights
```

---

## 3. Müsait Koltukları Listele

```bash
curl http://localhost:8080/api/v1/flights/1/seats
```

**Yanıt:**
```json
[
  { "id": 1, "seatNumber": "01", "price": 1500.00, "sold": false },
  { "id": 2, "seatNumber": "02", "price": 1500.00, "sold": false },
  { "id": 3, "seatNumber": "03", "price": 1500.00, "sold": false }
]
```

---

## 4. ✈️ Koltuk Satın Al (Iyzico Ödeme)

```bash
curl -X POST http://localhost:8080/api/v1/flights/1/seats/1/buy \
  -H "Content-Type: application/json" \
  -d '{
    "cardHolderName": "John Doe",
    "cardNumber": "5526080000000006",
    "expireMonth": "12",
    "expireYear": "2030",
    "cvc": "123"
  }'
```

> **Not:** `5526080000000006` Iyzico sandbox test kartıdır. Gerçek kart kullanmayın.

**Başarılı Yanıt (200 OK):**
```json
{
  "id": 1,
  "seatNumber": "01",
  "price": 1500.00,
  "sold": true
}
```

**Hata Yanıtları:**

| Durum | HTTP | Açıklama |
|-------|------|----------|
| Koltuk zaten satıldı | 409 Conflict | Başka biri aldı |
| Eş zamanlı çakışma | 409 Conflict | Optimistic Lock devreye girdi |
| Ödeme başarısız | 402 Payment Required | Kart reddedildi |
| Koltuk bulunamadı | 404 Not Found | Hatalı ID |

---

## 5. Uçuş Güncelle

```bash
curl -X PUT http://localhost:8080/api/v1/flights/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Istanbul - Ankara (Güncellendi)",
    "price": 1800.00
  }'
```

---

## 6. Uçuş Sil

```bash
curl -X DELETE http://localhost:8080/api/v1/flights/1
```

---

## 7. Koltuğa Manuel Koltuk Ekle

```bash
curl -X POST http://localhost:8080/api/v1/flights/1/seats \
  -H "Content-Type: application/json" \
  -d '{
    "seatNumber": "11",
    "price": 1200.00
  }'
```

---

## Iyzico Sandbox Test Kartları

| Kart No | Banka | Sonuç |
|---------|-------|-------|
| 5526080000000006 | Akbank | ✅ Başarılı |
| 4603450000000000 | Garanti | ✅ Başarılı |
| 5406670000000009 | Yapı Kredi | ❌ Başarısız |
| 4046460000000006 | Ziraat | ❌ Yetersiz Bakiye |

> Tüm test kartları için son kullanma tarihi: **12/2030**, CVC: **123**

---

## H2 Console (Veritabanı)

```
URL:      http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./data/demo
User:     root
Password: pass
```
