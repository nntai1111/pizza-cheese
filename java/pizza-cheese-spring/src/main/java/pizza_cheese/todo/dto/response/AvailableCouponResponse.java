package pizza_cheese.todo.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import pizza_cheese.todo.domain.Coupon;
import pizza_cheese.todo.domain.DiscountType;
import pizza_cheese.todo.dto.CodedEnumValue;

@Getter
@Setter
public class AvailableCouponResponse {

    private UUID id;
    private String code;
    private String description;
    private CodedEnumValue discountType;
    private String discountLabel;
    private BigDecimal minOrderValue;
    private BigDecimal orderAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    public static AvailableCouponResponse from(Coupon coupon, BigDecimal orderAmount, BigDecimal discountAmount) {
        AvailableCouponResponse response = new AvailableCouponResponse();
        response.setId(coupon.getId());
        response.setCode(coupon.getCode());
        response.setDescription(coupon.getDescription());
        response.setDiscountType(CodedEnumValue.from(coupon.getDiscountType()));
        response.setDiscountLabel(buildDiscountLabel(coupon));
        response.setMinOrderValue(coupon.getMinOrderValue());
        response.setOrderAmount(orderAmount);
        response.setDiscountAmount(discountAmount);
        response.setFinalAmount(orderAmount.subtract(discountAmount));
        return response;
    }

    private static String buildDiscountLabel(Coupon coupon) {
        if (coupon.getDiscountType() == DiscountType.PERCENT) {
            String label = "Giảm " + coupon.getDiscountValue().stripTrailingZeros().toPlainString() + "%";
            if (coupon.getMaxDiscount() != null) {
                label += " (tối đa " + formatVnd(coupon.getMaxDiscount()) + ")";
            }
            return label;
        }
        return "Giảm " + formatVnd(coupon.getDiscountValue());
    }

    private static String formatVnd(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString() + "đ";
    }
}
