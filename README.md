Dưới đây là một plan khá thực tế theo hướng enterprise-level cho ứng dụng Fullstack Pizza Store sử dụng:
•	Backend: Spring Boot
•	Frontend: Angular
•	Database: PostgreSQL
•	Authentication: JWT + RBAC
•	Architecture: Monolith trước → có thể scale sang microservice sau
________________________________________
1. Mục tiêu hệ thống
Xây dựng hệ thống Pizza Store gồm:
Khách hàng (Customer)
•	Xem menu
•	Tìm kiếm món ăn
•	Thêm vào giỏ hàng
•	Đặt hàng
•	Thanh toán online
•	Theo dõi trạng thái đơn hàng realtime
•	Xem lịch sử đơn hàng
________________________________________
Nhân viên cửa hàng
Kitchen Staff (Bếp)
•	Xem đơn đã confirmed
•	Chuyển trạng thái:
o	Preparing
o	Ready
o	Out for delivery
________________________________________
Delivery Staff
•	Xem đơn cần giao
•	Nhận đơn
•	Đánh dấu delivered
________________________________________
Cashier
•	Confirm thanh toán tiền mặt
•	Confirm đơn hàng
________________________________________
Admin
•	Quản lý users
•	Quản lý roles
•	CRUD menu
•	Quản lý coupon
•	Dashboard doanh thu
•	Quản lý trạng thái order
________________________________________
2. Tech Stack đề xuất
Backend
Component	Technology
Language	Java 8
Framework	Spring Boot
Security	Spring Security + JWT
Validation	Bean Validation
Mapping	MapStruct
Build Tool	Maven
Database	SQL SERVER
Cache	Redis
API Docs	Swagger/OpenAPI
Realtime	WebSocket + STOMP
File Storage	S3/MinIO
Async	RabbitMQ hoặc Kafka
Testing	JUnit + Mockito + Testcontainers
________________________________________
Frontend
Component	Technology
Framework	Angular LTS
State Management	NgRx hoặc Signals
UI	Angular Material, Tailwind
Auth	JWT
Realtime	SockJS + STOMP
Charts	Chart.js
Forms	Reactive Forms
HTTP	HttpClient + Interceptor
________________________________________
3. Role-Based Access Control (RBAC)
Roles
Role	Quyền
CUSTOMER	Đặt hàng
CASHIER	Confirm payment
KITCHEN	Xử lý món
DELIVERY	Giao hàng
ADMIN	Full quyền
________________________________________
4. Các module chính
MODULE 1 — Authentication
Chức năng (jjwt library vs oracle jwt library)
•	Register
•	Login
•	Refresh token
•	Logout
•	Forgot password
•	Email verification
APIs
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
________________________________________
MODULE 2 — Menu Management
Chức năng
•	CRUD pizza
•	CRUD categories
•	Upload image
•	Topping management
•	Combo management
Entity
Category
Pizza
Topping
Combo
________________________________________
MODULE 3 — Cart
Chức năng
•	Add to cart
•	Remove item
•	Update quantity
•	Apply coupon
Database
cart
cart_item
________________________________________
MODULE 4 — Order Management
Order Flow
PENDING_PAYMENT
        ↓
PAID
        ↓
CONFIRMED
        ↓
PREPARING
        ↓
READY
        ↓
OUT_FOR_DELIVERY
        ↓
DELIVERED
________________________________________
Chức năng
Customer
•	Create order
•	Cancel order
•	Track order
Kitchen
•	Accept preparing
•	Ready status
Delivery
•	Pick order
•	Delivered
________________________________________
Entities
orders
order_items
order_status_history
________________________________________
MODULE 5 — Payment
Payment methods
•	COD
•	VNPay
•	Momo
•	Stripe
________________________________________
Payment Flow
Create Order
    ↓
Create Payment URL
    ↓
Customer Payment
    ↓
Payment Callback
    ↓
Update Order Status
________________________________________
MODULE 6 — Realtime Notification
Use WebSocket
Realtime updates
•	Order status changed
•	Kitchen receives new order
•	Delivery receives new order
•	Customer sees realtime progress
________________________________________
MODULE 7 — Admin Dashboard
Features
•	Revenue chart
•	Top-selling pizzas
•	Total orders
•	Active users
•	Kitchen performance
________________________________________
5. Database Design (suggestion)
User Tables
users
roles
user_roles
addresses
________________________________________
Product Tables
categories
pizzas
pizza_images
toppings
pizza_toppings
________________________________________
Order Tables
orders
order_items
order_status_history
payments
coupons
________________________________________
Audit Tables
audit_logs
________________________________________
6. Màn hình hệ thống
CUSTOMER
Public
•	Home
•	Pizza list
•	Pizza detail
•	Search
•	Cart
Authenticated
•	Checkout
•	Payment
•	Order tracking
•	Order history
•	Profile
________________________________________
KITCHEN DASHBOARD
Screens
•	Confirmed orders
•	Preparing orders
•	Ready orders
Actions
CONFIRMED → PREPARING
PREPARING → READY
________________________________________
DELIVERY DASHBOARD
Screens
•	Orders waiting delivery
•	My deliveries
•	Delivery history
Actions
READY → OUT_FOR_DELIVERY
OUT_FOR_DELIVERY → DELIVERED
________________________________________
ADMIN DASHBOARD
Screens
•	User management
•	Menu management
•	Analytics
•	Coupons
•	Revenue reports
________________________________________
7. Flow hoàn chỉnh của hệ thống
Customer creates order
        ↓
Cashier confirms payment
        ↓
Kitchen receives order
        ↓
Kitchen prepares pizza
        ↓
Kitchen marks READY
        ↓
Delivery receives order
        ↓
Delivery delivers order
        ↓
Customer receives completed notification
________________________________________
8. Hướng phát triển nâng cao
Sau này có thể mở rộng
•	Microservice architecture
•	Mobile app
•	AI recommendation
•	Loyalty points
•	Inventory management
•	Real delivery tracking map
•	Multi-language

