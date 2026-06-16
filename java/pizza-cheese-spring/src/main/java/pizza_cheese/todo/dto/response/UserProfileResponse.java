package pizza_cheese.todo.dto.response;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import pizza_cheese.todo.domain.Role;
import pizza_cheese.todo.domain.User;

@Getter
@Setter
public class UserProfileResponse {

    private UUID id;
    private String username;
    private String email;
    private String name;
    private String phone;
    private String avatarUrl;
    private Set<Role> roles;
    private Instant createdAt;
    private Instant updatedAt;

    public static UserProfileResponse from(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setRoles(user.getRoles());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
