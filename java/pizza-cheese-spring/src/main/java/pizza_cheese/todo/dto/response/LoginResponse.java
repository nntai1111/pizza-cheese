package pizza_cheese.todo.dto.response;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private Instant expiresAt;
    private long refreshExpiresIn;
    private Instant refreshExpiresAt;
    private UserProfileResponse user;
}
