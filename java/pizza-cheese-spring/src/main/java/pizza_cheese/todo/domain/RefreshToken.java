package pizza_cheese.todo.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshToken {

    private UUID id;
    private String token;
    private UUID userId;
    private User user;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
