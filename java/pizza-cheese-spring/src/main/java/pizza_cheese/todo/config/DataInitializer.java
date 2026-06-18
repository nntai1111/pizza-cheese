package pizza_cheese.todo.config;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import pizza_cheese.todo.domain.Role;
import pizza_cheese.todo.domain.User;
import pizza_cheese.todo.dao.UserDao;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final String defaultAvatarUrl;

    public DataInitializer(
            UserDao userDao,
            PasswordEncoder passwordEncoder,
            @Value("${app.user.default-avatar-url}") String defaultAvatarUrl) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.defaultAvatarUrl = defaultAvatarUrl;
    }

    @Override
    public void run(String... args) {
        if (userDao.count() == 0) {
            seedUser("admin", "admin@hoidanit.vn", "Admin User", "123456", Set.of(Role.ADMIN));
            seedUser("cashier", "cashier@hoidanit.vn", "Cashier User", "123456", Set.of(Role.CASHIER));
            seedUser("user", "user@hoidanit.vn", "Normal User", "123456", Set.of(Role.CUSTOMER));
            log.info("Seeded default users into database");
        }
    }

    private void seedUser(String username, String email, String fullName, String rawPassword, Set<Role> roles) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setAvatarUrl(defaultAvatarUrl);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRoles(roles);
        userDao.save(user);
    }
}
