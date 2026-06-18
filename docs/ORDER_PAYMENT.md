# Order & Payment — Pizza Cheese

Tài liệu mô tả luồng đặt hàng, thanh toán VNPay và cách chạy thử trên môi trường local.

---

## 1. Tổng quan

| Thành phần | Công nghệ | Ghi chú |
|------------|-----------|---------|
| Backend | Spring Boot 3.4 + JDBC | `/api/v1/orders`, `/api/v1/payments/vnpay` |
| Frontend | Angular 22 | Checkout, payment return, theo dõi đơn |
| Database | PostgreSQL (Flyway V1) | Bảng `orders`, `payments`, `order_items` |
| Thanh toán | VNPay Sandbox | Redirect + IPN callback |

### Phương thức thanh toán hỗ trợ

| Method | Luồng |
|--------|-------|
| **VNPAY** | Tạo đơn `PENDING_PAYMENT` → redirect VNPay → IPN/Return → `CONFIRMED` + `PAID` |
| **COD** | Tạo đơn `CONFIRMED` + payment `PENDING` → không qua cổng |

---

## 2. Luồng nghiệp vụ

### 2.1 VNPay (Online)

```
Customer checkout
    → POST /api/v1/orders (paymentMethod=VNPAY)
    → Order: PENDING_PAYMENT
    → Payment: PENDING + paymentUrl
    → Giỏ hàng được xóa
    → Redirect sang VNPay

VNPay thanh toán OK
    → IPN: GET /api/v1/payments/vnpay/ipn?vnp_...
    → Verify HMAC → Payment PAID, Order CONFIRMED
    → Return: GET /api/v1/payments/vnpay/return → redirect FE

Customer xem kết quả
    → /customer/payment/return?vnp_...
    → GET /api/v1/payments/vnpay/status?txnRef=...
```

### 2.2 COD

```
Customer checkout (paymentMethod=COD)
    → Order: CONFIRMED
    → Payment: PENDING
    → Redirect /customer/orders/{id}
```

### 2.3 Hủy đơn (Customer)

| Trạng thái | Được hủy? |
|------------|-----------|
| `PENDING_PAYMENT` | Có |
| `CONFIRMED` + payment `PENDING` (COD) | Có |
| `CONFIRMED` + payment `PAID` (VNPay) | Không |
| `PREPARING` trở đi | Không |

API: `POST /api/v1/orders/{id}/cancel`

---

## 3. API Backend

Base URL: `http://localhost:8080/api/v1`

Tất cả endpoint order yêu cầu JWT + role `CUSTOMER`, trừ VNPay IPN/Return.

### Orders

| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/orders` | Tạo đơn từ giỏ hàng |
| GET | `/orders` | Danh sách đơn của tôi |
| GET | `/orders/{id}` | Chi tiết đơn |
| POST | `/orders/{id}/cancel` | Hủy đơn |

**Request tạo đơn:**

```json
{
  "paymentMethod": "VNPAY",
  "note": "Giao trước 12h",
  "deliveryAddress": {
    "recipientName": "Nguyễn Văn A",
    "phone": "0901234567",
    "addressLine1": "123 Đường ABC",
    "addressLine2": "Căn 501",
    "ward": "Phường 1",
    "district": "Quận 1",
    "city": "TP.HCM"
  }
}
```

**Response (rút gọn):**

```json
{
  "statusCode": 200,
  "message": "Success",
  "data": {
    "id": "uuid",
    "orderCode": "PC2506181234",
    "status": "PENDING_PAYMENT",
    "paymentMethod": "VNPAY",
    "paymentStatus": "PENDING",
    "finalAmount": 150000,
    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
    "items": [...]
  }
}
```

### VNPay

| Method | Path | Auth | Mô tả |
|--------|------|------|-------|
| GET | `/payments/vnpay/ipn` | Public | VNPay gọi server-to-server |
| GET | `/payments/vnpay/return` | Public | Browser return → redirect FE |
| GET | `/payments/vnpay/status?txnRef=` | JWT | Tra cứu đơn theo mã giao dịch |

---

## 4. Cấu hình VNPay

File: `java/pizza-cheese-spring/src/main/resources/application.properties`

```properties
app.vnpay.tmn-code=FCR0JH8M
app.vnpay.hash-secret=17W6NZM6YJ35QKQ1O42636HAPRLGB7FX
app.vnpay.pay-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
app.vnpay.return-url=http://localhost:8080/api/v1/payments/vnpay/return
app.vnpay.ipn-url=http://localhost:8080/api/v1/payments/vnpay/ipn
```

### Lưu ý IPN local

VNPay sandbox cần **URL IPN public (HTTPS)**. Khi dev local:

1. Dùng [ngrok](https://ngrok.com/) hoặc Cloudflare Tunnel
2. Cập nhật `app.vnpay.ipn-url` = `https://xxx.ngrok.io/api/v1/payments/vnpay/ipn`
3. Restart backend

Return URL có thể chạy qua `localhost:8080` vì trình duyệt khách redirect được.

### Thẻ test VNPay Sandbox

Tra cứu tại: https://sandbox.vnpayment.vn/dev/index.html

Thông thường dùng ngân hàng NCB, số thẻ và OTP theo hướng dẫn sandbox.

---

## 5. Frontend

### Routes mới

| URL | Trang |
|-----|-------|
| `/customer/checkout` | Form địa chỉ + chọn VNPay/COD |
| `/customer/payment/return` | Kết quả sau VNPay |
| `/customer/orders` | Danh sách đơn |
| `/customer/orders/:id` | Chi tiết + hủy đơn |

### Luồng UI

1. Giỏ hàng → **Tiến hành thanh toán**
2. Checkout → **Đặt hàng**
3. VNPay: `window.location.href = paymentUrl`
4. Sau thanh toán: VNPay → backend return → `/customer/payment/return`
5. Theo dõi: menu **Đơn hàng**

### Files chính

```
angular/.../core/models/order.model.ts
angular/.../core/services/order.service.ts
angular/.../features/customer/checkout/
angular/.../features/customer/payment-return/
angular/.../features/customer/order-list/
angular/.../features/customer/order-detail/
```

---

## 6. Backend — cấu trúc code

```
domain/OrderStatus, PaymentStatus, PaymentMethod, Order, Payment, ...
dao/OrderDao, PaymentDao
sql/order.sql, payment.sql
service/OrderService, VnPayService
controller/OrderController, VnPayController
config/VnPayProperties
util/VnPayUtil
```

---

## 7. Chạy thử

### Backend

```bash
cd java/pizza-cheese-spring
./mvnw spring-boot:run
```

### Frontend

```bash
cd angular/pizza-cheese-angular
npm start
```

### Checklist test

1. Đăng nhập customer
2. Thêm pizza/combo vào giỏ
3. Checkout → chọn VNPay → đặt hàng
4. Thanh toán sandbox
5. Kiểm tra trang return + đơn ở `/customer/orders`
6. Test COD: đặt đơn → status `CONFIRMED` ngay
7. Test hủy: đơn `PENDING_PAYMENT` hoặc COD `CONFIRMED`

---

## 8. Bảo mật

- Chỉ đánh dấu `PAID` khi verify HMAC VNPay thành công
- IPN xử lý idempotent (gọi nhiều lần vẫn OK)
- So khớp `vnp_Amount` với `final_amount × 100`
- **Không commit secret production** — dùng biến môi trường khi deploy

---

## 9. Việc làm tiếp theo (chưa implement)

- Kitchen: `CONFIRMED → PREPARING → READY`
- Delivery: `READY → OUT_FOR_DELIVERY → DELIVERED`
- Cashier confirm chuyển khoản thủ công
- Job timeout hủy đơn `PENDING_PAYMENT` quá hạn
- Quản lý địa chỉ (`addresses` table)
