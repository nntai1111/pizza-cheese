package pizza_cheese.todo.config;

import java.math.BigDecimal;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import pizza_cheese.todo.dao.CouponDao;
import pizza_cheese.todo.domain.Coupon;
import pizza_cheese.todo.domain.DiscountType;
import pizza_cheese.todo.domain.Role;
import pizza_cheese.todo.domain.User;
import pizza_cheese.todo.dao.UserDao;

@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserDao userDao;
    private final CouponDao couponDao;
    private final PasswordEncoder passwordEncoder;
    private final String defaultAvatarUrl;

    public DataInitializer(
            UserDao userDao,
            CouponDao couponDao,
            PasswordEncoder passwordEncoder,
            AppProperties appProperties) {
        this.userDao = userDao;
        this.couponDao = couponDao;
        this.passwordEncoder = passwordEncoder;
        this.defaultAvatarUrl = appProperties.getUser().getDefaultAvatarUrl();
    }

    @Override
    public void run(String... args) {
        if (userDao.count() == 0) {
            seedUser("admin", "admin@hoidanit.vn", "Admin User", "123456", Set.of(Role.ADMIN));
            seedUser("cashier", "cashier@hoidanit.vn", "Cashier User", "123456", Set.of(Role.CASHIER));
            seedUser("user", "user@hoidanit.vn", "Normal User", "123456", Set.of(Role.CUSTOMER));
            log.info("Seeded default users into database");
        }

        if (couponDao.count() == 0) {
            seedCoupon("WELCOME10", "Giảm 10% cho đơn từ 100.000đ", DiscountType.PERCENT,
                    new BigDecimal("10"), new BigDecimal("100000"), new BigDecimal("50000"), 100, 1);
            seedCoupon("SAVE50K", "Giảm 50.000đ cho đơn từ 200.000đ", DiscountType.FIXED,
                    new BigDecimal("50000"), new BigDecimal("200000"), null, 50, null);
            log.info("Seeded default coupons into database");
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

    private void seedCoupon(
            String code,
            String description,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal minOrderValue,
            BigDecimal maxDiscount,
            Integer usageLimit,
            Integer perUserLimit) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDescription(description);
        coupon.setDiscountType(discountType);
        coupon.setDiscountValue(discountValue);
        coupon.setMinOrderValue(minOrderValue);
        coupon.setMaxDiscount(maxDiscount);
        coupon.setUsageLimit(usageLimit);
        coupon.setUsedCount(0);
        coupon.setPerUserLimit(perUserLimit);
        coupon.setActive(true);
        couponDao.save(coupon);
    }
}
