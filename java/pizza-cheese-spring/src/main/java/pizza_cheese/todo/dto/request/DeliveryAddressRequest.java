package pizza_cheese.todo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryAddressRequest {

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255, message = "Địa chỉ quá dài")
    private String addressLine1;

    @Size(max = 255, message = "Địa chỉ phụ quá dài")
    private String addressLine2;

    @Size(max = 100, message = "Phường/xã quá dài")
    private String ward;

    @Size(max = 100, message = "Quận/huyện quá dài")
    private String district;

    @NotBlank(message = "Thành phố không được để trống")
    @Size(max = 100, message = "Thành phố quá dài")
    private String city;

    @NotBlank(message = "Số điện thoại nhận hàng không được để trống")
    @Size(max = 20, message = "Số điện thoại quá dài")
    private String phone;

    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(max = 100, message = "Tên người nhận quá dài")
    private String recipientName;
}
