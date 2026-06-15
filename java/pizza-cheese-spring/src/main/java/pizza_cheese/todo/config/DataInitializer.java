package pizza_cheese.todo.config;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import pizza_cheese.todo.domain.Role;
import pizza_cheese.todo.domain.User;
import pizza_cheese.todo.dao.UserDao;

@Component
public class DataInitializer implements CommandLineRunner {
    // CommandLineRunner: code trong run() sẽ chạy ngay khi app khởi động
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userDao.count() == 0) {
            seedUser("admin@hoidanit.vn", "Admin User", "123456", Set.of(Role.ADMIN, Role.USER));
            seedUser("user@hoidanit.vn", "Normal User", "123456", Set.of(Role.USER));
            log.info("Seeded default users into database");
        }
    }

    private void seedUser(String email, String name, String rawPassword, Set<Role> roles) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRoles(roles);
        userDao.save(user);
    }
}
