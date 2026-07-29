# ==============================================================================
# STAGE 1: Build ứng dụng Angular với Node.js 20
# ==============================================================================
FROM node:22-alpine AS builder

WORKDIR /app

# Copy package files để tận dụng cache npm install
COPY package*.json ./
RUN npm ci

# Copy toàn bộ source code Angular và build sản phẩm production
COPY . .
RUN npm run build -- --configuration production

# ==============================================================================
# STAGE 2: Phục vụ ứng dụng tĩnh bằng Web Server Nginx Alpine
# ==============================================================================
FROM nginx:alpine

# Copy kết quả build từ Stage 1 vào thư mục gốc của Nginx (Angular 17/18+ output tại dist/pizza-cheese/browser)
COPY --from=builder /app/dist/pizza-cheese/browser /usr/share/nginx/html

# Copy file cấu hình Nginx custom vào container
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
