package pizza_cheese.todo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Token đặt lại mật khẩu là bắt buộc")
    private String resetToken;

    @NotBlank(message = "Mật khẩu mới là bắt buộc")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String newPassword;
}
