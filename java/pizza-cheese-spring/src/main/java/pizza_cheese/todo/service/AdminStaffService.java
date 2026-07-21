package pizza_cheese.todo.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pizza_cheese.todo.dao.UserDao;
import pizza_cheese.todo.domain.Role;
import pizza_cheese.todo.domain.User;
import pizza_cheese.todo.dto.request.CreateStaffRequest;
import pizza_cheese.todo.dto.request.UpdateStaffRequest;
import pizza_cheese.todo.dto.response.PageResponse;
import pizza_cheese.todo.dto.response.UserProfileResponse;
import pizza_cheese.todo.exception.ApiException;

@Service
public class AdminStaffService {

    private static final Set<Role> STAFF_ROLES = EnumSet.of(
            Role.CASHIER, Role.KITCHEN, Role.DELIVERY, Role.ADMIN);

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public AdminStaffService(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResponse<UserProfileResponse> getStaff(Role role, int page, int size) {
        if (role != null) {
            validateStaffRole(role);
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        long total = userDao.countStaff(role);
        List<User> users = userDao.findStaffPage(role, safePage, safeSize);
        List<UserProfileResponse> content = users.stream().map(UserProfileResponse::from).toList();
        return PageResponse.of(content, safePage, safeSize, total);
    }

    public UserProfileResponse getStaffById(UUID id) {
        User user = requireStaffUser(id);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse createStaff(CreateStaffRequest request) {
        validateStaffRole(request.getRole());

        String username = normalizeUsername(request.getUsername());
        if (userDao.existsByUsername(username)) {
            throw ApiException.conflict("Tên đăng nhập đã được sử dụng");
        }
        if (userDao.existsByEmail(request.getEmail().trim().toLowerCase(Locale.ROOT))) {
            throw ApiException.conflict("Email đã được sử dụng");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone().trim());
        user.setRoles(Set.of(request.getRole()));
        user.setActive(true);
        userDao.save(user);
        return UserProfileResponse.from(userDao.findById(user.getId()).orElse(user));
    }

    @Transactional
    public UserProfileResponse updateStaff(UUID id, UpdateStaffRequest request, String actorEmail) {
        User user = requireStaffUser(id);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRole() != null) {
            validateStaffRole(request.getRole());
            user.setRoles(Set.of(request.getRole()));
        }
        if (request.getActive() != null) {
            if (!request.getActive() && user.getEmail().equalsIgnoreCase(actorEmail)) {
                throw ApiException.badRequest("Không thể khóa tài khoản đang đăng nhập");
            }
            user.setActive(request.getActive());
        }

        userDao.save(user);
        return UserProfileResponse.from(userDao.findById(user.getId()).orElse(user));
    }

    @Transactional
    public UserProfileResponse setActive(UUID id, boolean active, String actorEmail) {
        User user = requireStaffUser(id);
        if (!active && user.getEmail().equalsIgnoreCase(actorEmail)) {
            throw ApiException.badRequest("Không thể khóa tài khoản đang đăng nhập");
        }
        user.setActive(active);
        userDao.save(user);
        return UserProfileResponse.from(userDao.findById(user.getId()).orElse(user));
    }

    private User requireStaffUser(UUID id) {
        User user = userDao.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy nhân viên"));
        boolean isStaff = user.getRoles().stream().anyMatch(STAFF_ROLES::contains);
        if (!isStaff) {
            throw ApiException.notFound("Không tìm thấy nhân viên");
        }
        return user;
    }

    private void validateStaffRole(Role role) {
        if (role == null || !STAFF_ROLES.contains(role)) {
            throw ApiException.badRequest("Vai trò không hợp lệ cho nhân viên");
        }
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
