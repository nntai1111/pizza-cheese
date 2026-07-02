package pizza_cheese.todo.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import pizza_cheese.todo.domain.DiscountType;

@Getter
@Setter
public class UpdateCouponRequest {

    @Size(max = 50, message = "Mã coupon không được vượt quá 50 ký tự")
    private String code;

    @Size(max = 500, message = "Mô tả quá dài")
    private String description;

    private DiscountType discountType;

    @DecimalMin(value = "0", inclusive = false, message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0", message = "Giá trị đơn tối thiểu không được âm")
    private BigDecimal minOrderValue;

    @DecimalMin(value = "0", message = "Giảm tối đa không được âm")
    private BigDecimal maxDiscount;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Min(value = 1, message = "Giới hạn sử dụng phải >= 1")
    private Integer usageLimit;

    @Min(value = 1, message = "Giới hạn mỗi user phải >= 1")
    private Integer perUserLimit;

    private Boolean isActive;
}
