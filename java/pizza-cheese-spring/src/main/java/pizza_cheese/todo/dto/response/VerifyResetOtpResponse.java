package pizza_cheese.todo.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyResetOtpResponse {

    private String resetToken;
    private long expiresIn;
    private Instant expiresAt;
    private String message;
}
