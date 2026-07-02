package pizza_cheese.todo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.Getter;
import pizza_cheese.todo.dao.CouponDao;
import pizza_cheese.todo.dao.UserDao;
import pizza_cheese.todo.domain.Coupon;
import pizza_cheese.todo.domain.DiscountType;
import pizza_cheese.todo.dto.request.CreateCouponRequest;
import pizza_cheese.todo.dto.request.UpdateCouponRequest;
import pizza_cheese.todo.dto.response.AvailableCouponResponse;
import pizza_cheese.todo.dto.response.CouponResponse;
import pizza_cheese.todo.exception.ApiException;

@Service
public class CouponService {

    private final CouponDao couponDao;
    private final UserDao userDao;

    public CouponService(CouponDao couponDao, UserDao userDao) {
        this.couponDao = couponDao;
        this.userDao = userDao;
    }

    public List<CouponResponse> findAll(boolean activeOnly) {
        return couponDao.findAll(activeOnly).stream().map(CouponResponse::from).toList();
    }

    public CouponResponse findById(UUID id) {
        return couponDao.findById(id)
                .map(CouponResponse::from)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy coupon"));
    }

    @Transactional
    public CouponResponse create(CreateCouponRequest request) {
        String code = normalizeCode(request.getCode());
        if (couponDao.findByCode(code).isPresent()) {
            throw ApiException.conflict("Mã coupon đã tồn tại");
        }

        validateDiscountRules(request.getDiscountType(), request.getDiscountValue());
        validateDateRange(request.getStartDate(), request.getEndDate());

        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDescription(trimToNull(request.getDescription()));
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderValue(request.getMinOrderValue());
        coupon.setMaxDiscount(request.getMaxDiscount());
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setUsedCount(0);
        coupon.setPerUserLimit(request.getPerUserLimit());
        coupon.setActive(request.getIsActive() == null || request.getIsActive());

        return CouponResponse.from(couponDao.save(coupon));
    }

    @Transactional
    public CouponResponse update(UUID id, UpdateCouponRequest request) {
        Coupon coupon = couponDao.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy coupon"));

        if (request.getCode() != null) {
            String code = normalizeCode(request.getCode());
            couponDao.findByCode(code)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw ApiException.conflict("Mã coupon đã tồn tại");
                    });
            coupon.setCode(code);
        }
        if (request.getDescription() != null) {
            coupon.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getDiscountType() != null) {
            coupon.setDiscountType(request.getDiscountType());
        }
        if (request.getDiscountValue() != null) {
            coupon.setDiscountValue(request.getDiscountValue());
        }
        if (request.getMinOrderValue() != null) {
            coupon.setMinOrderValue(request.getMinOrderValue());
        }
        if (request.getMaxDiscount() != null) {
            coupon.setMaxDiscount(request.getMaxDiscount());
        }
        if (request.getStartDate() != null) {
            coupon.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            coupon.setEndDate(request.getEndDate());
        }
        if (request.getUsageLimit() != null) {
            coupon.setUsageLimit(request.getUsageLimit());
        }
        if (request.getPerUserLimit() != null) {
            coupon.setPerUserLimit(request.getPerUserLimit());
        }
        if (request.getIsActive() != null) {
            coupon.setActive(request.getIsActive());
        }

        validateDiscountRules(coupon.getDiscountType(), coupon.getDiscountValue());
        validateDateRange(coupon.getStartDate(), coupon.getEndDate());

        return CouponResponse.from(couponDao.save(coupon));
    }

    @Transactional
    public void delete(UUID id) {
        if (couponDao.findById(id).isEmpty()) {
            throw ApiException.notFound("Không tìm thấy coupon");
        }
        couponDao.deactivate(id);
    }

    public CouponApplyResult applyCoupon(String userEmail, String couponCode, BigDecimal orderAmount) {
        if (couponCode == null || couponCode.isBlank()) {
            return CouponApplyResult.noDiscount(orderAmount);
        }

        UUID userId = resolveUserId(userEmail);
        Coupon coupon = couponDao.findByCode(normalizeCode(couponCode))
                .orElseThrow(() -> ApiException.badRequest("Mã giảm giá không hợp lệ"));

        validateCoupon(coupon, userId, orderAmount);
        BigDecimal discountAmount = calculateDiscount(coupon, orderAmount);
        BigDecimal finalAmount = orderAmount.subtract(discountAmount);

        return new CouponApplyResult(coupon, orderAmount, discountAmount, finalAmount);
    }

    public List<AvailableCouponResponse> findAvailableCoupons(String userEmail, BigDecimal orderAmount) {
        UUID userId = resolveUserId(userEmail);
        return couponDao.findAll(true).stream()
                .filter(coupon -> isEligible(coupon, userId, orderAmount))
                .map(coupon -> AvailableCouponResponse.from(
                        coupon, orderAmount, calculateDiscount(coupon, orderAmount)))
                .sorted(Comparator.comparing(AvailableCouponResponse::getDiscountAmount).reversed())
                .toList();
    }

    @Transactional
    public void recordUsage(UUID couponId, UUID userId, UUID orderId) {
        if (!couponDao.incrementUsedCount(couponId)) {
            throw ApiException.conflict("Mã giảm giá đã hết lượt sử dụng");
        }
        couponDao.insertUsage(couponId, userId, orderId);
    }

    public String findCodeById(UUID couponId) {
        return couponDao.findById(couponId).map(Coupon::getCode).orElse(null);
    }

    private void validateCoupon(Coupon coupon, UUID userId, BigDecimal orderAmount) {
        if (!isEligible(coupon, userId, orderAmount)) {
            throw ApiException.badRequest(getIneligibilityMessage(coupon, userId, orderAmount));
        }
    }

    private boolean isEligible(Coupon coupon, UUID userId, BigDecimal orderAmount) {
        if (!coupon.isActive()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
            return false;
        }
        if (coupon.getEndDate() != null && now.isAfter(coupon.getEndDate())) {
            return false;
        }
        if (coupon.getMinOrderValue() != null && orderAmount.compareTo(coupon.getMinOrderValue()) < 0) {
            return false;
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return false;
        }
        if (coupon.getPerUserLimit() != null) {
            int userUsageCount = couponDao.countUsagesByUser(coupon.getId(), userId);
            if (userUsageCount >= coupon.getPerUserLimit()) {
                return false;
            }
        }
        return true;
    }

    private String getIneligibilityMessage(Coupon coupon, UUID userId, BigDecimal orderAmount) {
        if (!coupon.isActive()) {
            return "Mã giảm giá không còn hiệu lực";
        }

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
            return "Mã giảm giá chưa có hiệu lực";
        }
        if (coupon.getEndDate() != null && now.isAfter(coupon.getEndDate())) {
            return "Mã giảm giá đã hết hạn";
        }
        if (coupon.getMinOrderValue() != null && orderAmount.compareTo(coupon.getMinOrderValue()) < 0) {
            return "Đơn hàng chưa đạt giá trị tối thiểu để dùng mã";
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return "Mã giảm giá đã hết lượt sử dụng";
        }
        if (coupon.getPerUserLimit() != null) {
            int userUsageCount = couponDao.countUsagesByUser(coupon.getId(), userId);
            if (userUsageCount >= coupon.getPerUserLimit()) {
                return "Bạn đã sử dụng hết lượt cho mã này";
            }
        }
        return "Mã giảm giá không hợp lệ";
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PERCENT) {
            discount = orderAmount.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
                discount = coupon.getMaxDiscount();
            }
        } else {
            discount = coupon.getDiscountValue();
        }

        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }
        return discount;
    }

    private void validateDiscountRules(DiscountType discountType, BigDecimal discountValue) {
        if (discountType == DiscountType.PERCENT && discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw ApiException.badRequest("Phần trăm giảm giá không được vượt quá 100%");
        }
    }

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw ApiException.badRequest("Ngày kết thúc phải sau ngày bắt đầu");
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UUID resolveUserId(String userEmail) {
        return userDao.findByEmail(userEmail)
                .map(pizza_cheese.todo.domain.User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
    }

    @Getter
    public static class CouponApplyResult {

        private final Coupon coupon;
        private final BigDecimal orderAmount;
        private final BigDecimal discountAmount;
        private final BigDecimal finalAmount;

        public CouponApplyResult(Coupon coupon, BigDecimal orderAmount, BigDecimal discountAmount, BigDecimal finalAmount) {
            this.coupon = coupon;
            this.orderAmount = orderAmount;
            this.discountAmount = discountAmount;
            this.finalAmount = finalAmount;
        }

        public static CouponApplyResult noDiscount(BigDecimal orderAmount) {
            return new CouponApplyResult(null, orderAmount, BigDecimal.ZERO, orderAmount);
        }

        public UUID getCouponId() {
            return coupon != null ? coupon.getId() : null;
        }
    }
}
