package pizza_cheese.todo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyResetOtpRequest {

    @NotBlank(message = "Email là bắt buộc")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mã OTP là bắt buộc")
    @Pattern(regexp = "^\\d{6}$", message = "Mã OTP phải gồm 6 chữ số")
    private String otp;
}
