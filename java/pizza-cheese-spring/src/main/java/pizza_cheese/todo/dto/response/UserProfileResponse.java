package pizza_cheese.todo.dto.response;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import pizza_cheese.todo.domain.User;
import pizza_cheese.todo.dto.CodedEnumValue;

@Getter
@Setter
public class UserProfileResponse {

    private UUID id;
    private String username;
    private String email;
    private String name;
    private String phone;
    private String avatarUrl;
    private Set<CodedEnumValue> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserProfileResponse from(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setRoles(CodedEnumValue.fromSet(user.getRoles()));
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
