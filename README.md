# Dummy Payment Service

E-ticaret uygulamalarının ödeme entegrasyonlarını geliştirmek ve test etmek için hazırlanmış basit bir ödeme sağlayıcısı simülasyonudur. Gerçek bir ödeme işlemi veya para transferi gerçekleştirmez.

Servis, ödeme talebini hemen `PROCESSING` durumuyla kabul eder. Yapılandırılabilir bir bekleme süresinin ardından rastgele `APPROVED`, `REJECTED` veya cevapsızlık senaryolarından birini seçer. Cevapsızlık senaryosunda ana uygulamaya callback göndermez.

## Teknolojiler

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Jakarta Bean Validation
- Maven Wrapper

## Çalışma Akışı

1. İstemci `POST /api/payments` endpoint'ine ödeme talebi gönderir.
2. Servis benzersiz bir `paymentId` üretir ve `202 Accepted` yanıtı döner.
3. Ödeme arka planda asenkron olarak işlenir.
4. Yapılandırılmış bekleme süresi tamamlandığında sonuç rastgele belirlenir.
5. Sonuç sağlayıcı tarafında saklanır. Normal senaryoda ana uygulamanın callback
   endpoint'ine gönderilir; cevapsızlık senaryosunda callback gönderilmez ve sonuç
   durum sorgulama endpoint'i üzerinden alınabilir.

## Gereksinimler

- JDK 21 veya üzeri
- Callback'i kabul edecek ana uygulama (entegrasyon testi için)

Maven'ın ayrıca kurulması gerekmez; proje Maven Wrapper içerir.

## Yapılandırma

Desteklenen ortam değişkenleri:

| Değişken                      | Varsayılan değer                                 | Açıklama                                         |
| ----------------------------- | ------------------------------------------------ | ------------------------------------------------ |
| `PAYMENT_CALLBACK_URL`        | `http://localhost:8080/api/v1/payments/callback` | Ödeme sonucunun gönderileceği endpoint           |
| `PAYMENT_PROCESSING_DELAY_MS` | `3000`                                           | Callback gönderilmeden önce beklenecek süre (ms) |

Örnek değerler `.env.example` dosyasında bulunur. Yerel ayarlar için bu dosyayı kopyalayabilirsiniz:

```bash
cp .env.example .env
```

Spring Boot `.env` dosyasını doğrudan yüklemediği için değişkenleri uygulamayı başlatmadan önce kabuğa aktarmalısınız:

```bash
set -a
source .env
set +a
```

Ortam değişkenleri tanımlanmazsa yukarıdaki varsayılan değerler kullanılır.

## Uygulamayı Çalıştırma

```bash
./mvnw spring-boot:run
```

Servis varsayılan olarak `http://localhost:8081` adresinde çalışır.

## API

### Ödeme Oluşturma

```http
POST /api/payments
Content-Type: application/json
Idempotency-Key: 22222222-2222-2222-2222-222222222222
```

Örnek istek:

```json
{
  "orderId": "42",
  "method": "credit_card",
  "paymentToken": "tok_test_123",
  "items": [
    {
      "productName": "Kablosuz Kulaklık",
      "quantity": 2,
      "unitPrice": 749.9
    }
  ],
  "currency": "TRY"
}
```

Desteklenen para birimleri: `TRY`, `USD` ve `EUR`.

Örnek `202 Accepted` yanıtı:

```json
{
  "paymentId": "e9126c96-5c9f-447e-8938-c824c5a89b7e",
  "orderId": "42",
  "idempotencyKey": "22222222-2222-2222-2222-222222222222",
  "status": "PROCESSING"
}
```

cURL örneği:

```bash
curl --request POST http://localhost:8081/api/payments \
  --header "Content-Type: application/json" \
  --header "Idempotency-Key: 22222222-2222-2222-2222-222222222222" \
  --data '{
    "orderId": "42",
    "method": "credit_card",
    "paymentToken": "tok_test_123",
    "items": [
      {
        "productName": "Kablosuz Kulaklık",
        "quantity": 2,
        "unitPrice": 749.90
      }
    ],
    "currency": "TRY"
  }'
```

Desteklenen ödeme yöntemleri `credit_card`, `bank_transfer` ve
`cash_on_delivery` değerleridir. `paymentToken` yalnızca `credit_card` için
zorunludur.

### İade

```http
POST /api/payments/{paymentId}/refunds
Content-Type: application/json
```

```json
{
  "method": "credit_card"
}
```

İade çağrısı aynı ödeme için idempotenttir ve `refundId` döndürür.

### Ödeme Durumu Sorgulama

Provider ödeme kimliğiyle sorgulama:

```http
GET /api/payments/{paymentId}
```

Provider yanıtı alınamadan bağlantı kesildiyse idempotency key ile sorgulama:

```http
GET /api/payments/by-idempotency-key/{idempotencyKey}
```

Her iki endpoint de güncel `PROCESSING`, `APPROVED` veya `REJECTED` durumunu,
sipariş ve ödeme tanımlayıcılarını, tutarı ve para birimini döndürür.

### Callback

İşlem tamamlandığında yapılandırılmış callback adresine aşağıdaki biçimde bir `POST` isteği gönderilir:

```json
{
  "paymentId": "e9126c96-5c9f-447e-8938-c824c5a89b7e",
  "orderId": "42",
  "idempotencyKey": "22222222-2222-2222-2222-222222222222",
  "status": "APPROVED",
  "totalAmount": 1499.8,
  "currency": "TRY",
  "message": "Payment approved"
}
```

Olası sonuçlar eşit olasılıkla `APPROVED`, `REJECTED` veya callback gönderilmeyen cevapsızlık senaryosudur.

## Testler

```bash
./mvnw test
```

## Entegrasyon

Bu servis, `ecommerce_management` uygulamasının yerel ödeme sağlayıcısı olarak tasarlanmıştır. İki uygulama birlikte çalıştırıldığında:

- `ecommerce_management`: `http://localhost:8080`
- `dummy-payment-service`: `http://localhost:8081`

Ana uygulamada dummy ödeme sağlayıcısının adresi şu ortam değişkeniyle değiştirilebilir:

```bash
DUMMY_PAYMENT_BASE_URL=http://localhost:8081
```
