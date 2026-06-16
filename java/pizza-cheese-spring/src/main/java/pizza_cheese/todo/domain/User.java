package pizza_cheese.todo.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {

    private UUID id;
    private String username;
    private String email;
    private String passwordHash;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private Set<Role> roles = new HashSet<>();
    private Instant createdAt;
    private Instant updatedAt;
}
