# Dummy Payment Service

E-ticaret uygulamalarının ödeme entegrasyonlarını geliştirmek ve test etmek için hazırlanmış basit bir ödeme sağlayıcısı simülasyonudur. Gerçek bir ödeme işlemi veya para transferi gerçekleştirmez.

Servis, ödeme talebini hemen `PROCESSING` durumuyla kabul eder. Yapılandırılabilir bir bekleme süresinin ardından sonucu rastgele `APPROVED` veya `REJECTED` olarak belirler ve ana uygulamaya HTTP callback gönderir.

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
5. Sonuç, ana uygulamanın callback endpoint'ine gönderilir.

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
```

Örnek istek:

```json
{
  "orderId": "42",
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
  "status": "PROCESSING"
}
```

cURL örneği:

```bash
curl --request POST http://localhost:8081/api/payments \
  --header "Content-Type: application/json" \
  --data '{
    "orderId": "42",
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

### Callback

İşlem tamamlandığında yapılandırılmış callback adresine aşağıdaki biçimde bir `POST` isteği gönderilir:

```json
{
  "paymentId": "e9126c96-5c9f-447e-8938-c824c5a89b7e",
  "orderId": "42",
  "status": "APPROVED",
  "totalAmount": 1499.8,
  "currency": "TRY",
  "message": "Payment approved"
}
```

Olası nihai durumlar `APPROVED` ve `REJECTED` değerleridir.

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
