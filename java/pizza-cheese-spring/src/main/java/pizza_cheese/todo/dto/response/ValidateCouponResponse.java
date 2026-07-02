package pizza_cheese.todo.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import pizza_cheese.todo.dto.CodedEnumValue;
import pizza_cheese.todo.service.CouponService.CouponApplyResult;

@Getter
@Setter
public class ValidateCouponResponse {

    private UUID couponId;
    private String code;
    private String description;
    private CodedEnumValue discountType;
    private BigDecimal orderAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    public static ValidateCouponResponse from(CouponApplyResult result) {
        ValidateCouponResponse response = new ValidateCouponResponse();
        response.setOrderAmount(result.getOrderAmount());
        response.setDiscountAmount(result.getDiscountAmount());
        response.setFinalAmount(result.getFinalAmount());

        if (result.getCoupon() != null) {
            response.setCouponId(result.getCoupon().getId());
            response.setCode(result.getCoupon().getCode());
            response.setDescription(result.getCoupon().getDescription());
            response.setDiscountType(CodedEnumValue.from(result.getCoupon().getDiscountType()));
        }

        return response;
    }
}
