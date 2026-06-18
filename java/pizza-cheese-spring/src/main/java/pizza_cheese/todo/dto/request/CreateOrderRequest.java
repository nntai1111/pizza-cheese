package pizza_cheese.todo.dto.request;

import jakarta.validation.Valid;
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
}
