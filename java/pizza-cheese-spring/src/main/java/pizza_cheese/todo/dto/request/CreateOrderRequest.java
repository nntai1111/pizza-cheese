package pizza_cheese.todo.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import pizza_cheese.todo.domain.PaymentMethod;

@Getter
@Setter
public class CreateOrderRequest {

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    @Valid
    @NotNull(message = "Địa chỉ giao hàng không được để trống")
    private DeliveryAddressRequest deliveryAddress;

    @Size(max = 500, message = "Ghi chú quá dài")
    private String note;

    @Size(max = 50, message = "Mã giảm giá quá dài")
    private String couponCode;

    @NotEmpty(message = "Phải chọn ít nhất một món trong giỏ hàng")
    private List<UUID> cartItemIds;
}
