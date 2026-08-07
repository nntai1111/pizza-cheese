package pizza_cheese.todo.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetToken {

    private UUID id;
    private UUID userId;
    private User user;
    private String token;
    private String otpHash;
    private LocalDateTime expiresAt;
    private boolean used;
    private int attempts;
    private LocalDateTime createdAt;

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
