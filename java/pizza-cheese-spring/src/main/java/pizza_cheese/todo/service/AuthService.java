package pizza_cheese.todo.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import pizza_cheese.todo.config.AppProperties;
import pizza_cheese.todo.dao.EmailVerificationTokenDao;
import pizza_cheese.todo.dao.PasswordResetTokenDao;
import pizza_cheese.todo.dao.RefreshTokenDao;
import pizza_cheese.todo.dao.UserDao;
import pizza_cheese.todo.domain.EmailVerificationToken;
import pizza_cheese.todo.domain.PasswordResetToken;
import pizza_cheese.todo.domain.RefreshToken;
import pizza_cheese.todo.domain.Role;
import pizza_cheese.todo.domain.User;
import pizza_cheese.todo.dto.request.LoginRequest;
import pizza_cheese.todo.dto.request.RegisterRequest;
import pizza_cheese.todo.dto.response.ForgotPasswordResponse;
import pizza_cheese.todo.dto.response.LoginResponse;
import pizza_cheese.todo.dto.response.MessageResponse;
import pizza_cheese.todo.dto.response.RegisterPendingResponse;
import pizza_cheese.todo.dto.response.UserProfileResponse;
import pizza_cheese.todo.dto.response.VerifyResetOtpResponse;
import pizza_cheese.todo.exception.ApiException;
import pizza_cheese.todo.util.SecurityUtil;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final String FORGOT_PASSWORD_GENERIC_MESSAGE = "Nếu email tồn tại trong hệ thống, chúng tôi đã gửi mã OTP. Vui lòng kiểm tra hộp thư.";

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final PasswordEncoder passwordEncoder;
    private final UserDao userDao;
    private final RefreshTokenDao refreshTokenDao;
    private final EmailVerificationTokenDao emailVerificationTokenDao;
    private final PasswordResetTokenDao passwordResetTokenDao;
    private final CloudinaryService cloudinaryService;
    private final EmailService emailService;
    private final AppProperties appProperties;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final long emailVerificationTokenExpiration;
    private final long passwordResetOtpExpiration;
    private final long passwordResetTokenExpiration;
    private final String defaultAvatarUrl;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            PasswordEncoder passwordEncoder,
            UserDao userDao,
            RefreshTokenDao refreshTokenDao,
            EmailVerificationTokenDao emailVerificationTokenDao,
            PasswordResetTokenDao passwordResetTokenDao,
            CloudinaryService cloudinaryService,
            EmailService emailService,
            AppProperties appProperties) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.passwordEncoder = passwordEncoder;
        this.userDao = userDao;
        this.refreshTokenDao = refreshTokenDao;
        this.emailVerificationTokenDao = emailVerificationTokenDao;
        this.passwordResetTokenDao = passwordResetTokenDao;
        this.cloudinaryService = cloudinaryService;
        this.emailService = emailService;
        this.appProperties = appProperties;
        this.accessTokenExpiration = appProperties.getJwt().getAccessTokenValidityInSeconds();
        this.refreshTokenExpiration = appProperties.getJwt().getRefreshTokenValidityInSeconds();
        this.emailVerificationTokenExpiration = appProperties.getEmailVerificationTokenValidityInSeconds();
        this.passwordResetOtpExpiration = appProperties.getPasswordResetOtpValidityInSeconds();
        this.passwordResetTokenExpiration = appProperties.getPasswordResetTokenValidityInSeconds();
        this.defaultAvatarUrl = appProperties.getUser().getDefaultAvatarUrl();
    }

    @Transactional
    public RegisterPendingResponse register(RegisterRequest request, MultipartFile avatar) {
        String username = normalizeUsername(request.getUsername());
        String email = normalizeEmail(request.getEmail());

        userDao.findByEmail(email).ifPresent(existing -> {
            if (existing.isEmailVerified()) {
                throw ApiException.conflict("Email đã được sử dụng");
            }
            userDao.deleteById(existing.getId());
        });

        if (userDao.existsByUsername(username)) {
            throw ApiException.conflict("Tên đăng nhập đã được sử dụng");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(resolveAvatar(avatar));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(Role.CUSTOMER));
        user.setEmailVerified(false);
        user.setActive(false);
        userDao.save(user);

        sendVerificationEmail(user);

        return new RegisterPendingResponse(
                email,
                "Vui lòng kiểm tra email để xác thực tài khoản");
    }

    @Transactional
    public MessageResponse verifyEmail(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw ApiException.badRequest("Token xác thực không hợp lệ");
        }

        EmailVerificationToken token = emailVerificationTokenDao.findByToken(tokenValue.trim())
                .orElseThrow(() -> ApiException.badRequest(
                        "Link xác thực không hợp lệ. Vui lòng đăng ký lại."));

        if (token.isExpired()) {
            User user = token.getUser();
            emailVerificationTokenDao.delete(token);
            if (user != null && !user.isEmailVerified()) {
                userDao.deleteById(user.getId());
            }
            throw ApiException.badRequest(
                    "Xác thực đã hết hạn, vui lòng đăng ký lại");
        }

        User user = token.getUser();
        if (user == null) {
            emailVerificationTokenDao.delete(token);
            throw ApiException.badRequest("Link xác thực không hợp lệ. Vui lòng đăng ký lại.");
        }

        if (user.isEmailVerified() && user.isActive()) {
            emailVerificationTokenDao.deleteByUser(user);
            return new MessageResponse("Tài khoản đã được xác thực trước đó. Vui lòng đăng nhập.");
        }

        user.setEmailVerified(true);
        user.setActive(true);
        userDao.save(user);
        emailVerificationTokenDao.deleteByUser(user);

        return new MessageResponse("Xác thực email thành công. Vui lòng đăng nhập.");
    }

    @Transactional
    public MessageResponse resendVerification(String emailRaw) {
        String email = normalizeEmail(emailRaw);
        User user = userDao.findByEmail(email)
                .orElseThrow(() -> ApiException.badRequest(
                        "Không tìm thấy tài khoản chưa xác thực với email này"));

        if (user.isEmailVerified()) {
            throw ApiException.badRequest("Email này đã được xác thực. Vui lòng đăng nhập.");
        }

        emailVerificationTokenDao.findLatestByUserId(user.getId()).ifPresent(latest -> {
            if (latest.getCreatedAt() != null
                    && latest.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(RESEND_COOLDOWN_SECONDS))) {
                throw ApiException.badRequest("Vui lòng đợi khoảng 1 phút trước khi gửi lại email");
            }
        });

        sendVerificationEmail(user);
        return new MessageResponse("Đã gửi lại email xác thực. Vui lòng kiểm tra hộp thư.");
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenDao.findByToken(refreshTokenValue)
                .ifPresent(refreshTokenDao::delete);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String login = request.getLogin().trim();

        userDao.findByEmailOrUsername(login).ifPresent(user -> {
            if (!user.isEmailVerified() || !user.isActive()) {
                if (passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                    if (!user.isEmailVerified()) {
                        throw ApiException.unauthorized("Tài khoản chưa xác thực email");
                    }
                    throw ApiException.unauthorized("Tài khoản đã bị khóa");
                }
            }
        });

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                login,
                request.getPassword());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(authToken);
        } catch (DisabledException ex) {
            throw ApiException.unauthorized("Tài khoản chưa xác thực email hoặc đã bị khóa");
        }

        User user = userDao.findByEmailOrUsername(login)
                .orElseThrow();

        refreshTokenDao.deleteByUser(user);

        LoginResponse response = buildTokenResponse(authentication, user);
        response.setUser(UserProfileResponse.from(user));
        return response;
    }

    @Transactional
    public LoginResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenDao.findByToken(refreshTokenValue)
                .orElseThrow(() -> ApiException.unauthorized("Refresh token không hợp lệ"));

        if (refreshToken.isExpired()) {
            refreshTokenDao.delete(refreshToken);
            throw ApiException.unauthorized("Refresh token đã hết hạn");
        }

        User user = refreshToken.getUser();
        if (user == null || !user.isActive() || !user.isEmailVerified()) {
            refreshTokenDao.delete(refreshToken);
            throw ApiException.unauthorized("Tài khoản không còn hiệu lực");
        }

        refreshTokenDao.delete(refreshToken);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                user.getRoles().stream()
                        .map(role -> (GrantedAuthority) () -> "ROLE_" + role.name())
                        .collect(Collectors.toList()));

        return buildTokenResponse(authToken, user);
    }

    public UserProfileResponse getProfile(String email) {
        return userDao.findByEmail(email)
                .map(UserProfileResponse::from)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với email: " + email));
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(String emailRaw) {
        String email = normalizeEmail(emailRaw);

        userDao.findByEmail(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                return;
            }

            boolean inCooldown = passwordResetTokenDao.findLatestByUserId(user.getId())
                    .map(latest -> latest.getCreatedAt() != null
                            && latest.getCreatedAt()
                                    .isAfter(LocalDateTime.now().minusSeconds(RESEND_COOLDOWN_SECONDS)))
                    .orElse(false);

            if (inCooldown) {
                return;
            }

            sendPasswordResetOtp(user);
        });

        return new ForgotPasswordResponse(email, FORGOT_PASSWORD_GENERIC_MESSAGE);
    }

    @Transactional
    public VerifyResetOtpResponse verifyResetOtp(String emailRaw, String otpRaw) {
        String email = normalizeEmail(emailRaw);
        String otp = otpRaw == null ? "" : otpRaw.trim();

        User user = userDao.findByEmail(email)
                .orElseThrow(() -> ApiException.badRequest("Mã OTP không hợp lệ hoặc đã hết hạn"));

        if (!user.isEmailVerified()) {
            throw ApiException.badRequest("Mã OTP không hợp lệ hoặc đã hết hạn");
        }

        PasswordResetToken resetToken = passwordResetTokenDao.findLatestByUserId(user.getId())
                .orElseThrow(() -> ApiException.badRequest("Mã OTP không hợp lệ hoặc đã hết hạn"));

        if (resetToken.isUsed() || resetToken.getToken() != null) {
            throw ApiException.badRequest("Mã OTP không hợp lệ hoặc đã hết hạn");
        }

        if (resetToken.isExpired()) {
            throw ApiException.badRequest("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        if (resetToken.getAttempts() >= MAX_OTP_ATTEMPTS) {
            throw ApiException.badRequest("Bạn đã nhập sai OTP quá nhiều lần. Vui lòng yêu cầu mã mới.");
        }

        if (resetToken.getOtpHash() == null || !passwordEncoder.matches(otp, resetToken.getOtpHash())) {
            passwordResetTokenDao.incrementAttempts(resetToken);
            int remaining = MAX_OTP_ATTEMPTS - resetToken.getAttempts();
            if (remaining <= 0) {
                throw ApiException.badRequest("Bạn đã nhập sai OTP quá nhiều lần. Vui lòng yêu cầu mã mới.");
            }
            throw ApiException.badRequest("Mã OTP không đúng. Còn " + remaining + " lần thử.");
        }

        Instant expiresAt = Instant.now().plus(passwordResetTokenExpiration, ChronoUnit.SECONDS);
        resetToken.setToken(generateSecureToken());
        resetToken.setExpiresAt(LocalDateTime.ofInstant(expiresAt, APP_ZONE));
        passwordResetTokenDao.updateAfterOtpVerified(resetToken);

        return new VerifyResetOtpResponse(
                resetToken.getToken(),
                passwordResetTokenExpiration,
                expiresAt,
                "Xác thực OTP thành công. Vui lòng đặt mật khẩu mới.");
    }

    @Transactional
    public MessageResponse resetPassword(String resetTokenValue, String newPassword) {
        if (resetTokenValue == null || resetTokenValue.isBlank()) {
            throw ApiException.badRequest("Token đặt lại mật khẩu không hợp lệ");
        }

        PasswordResetToken resetToken = passwordResetTokenDao.findByToken(resetTokenValue.trim())
                .orElseThrow(() -> ApiException.badRequest("Token đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"));

        if (resetToken.isUsed()) {
            throw ApiException.badRequest("Token đặt lại mật khẩu đã được sử dụng");
        }

        if (resetToken.isExpired()) {
            throw ApiException.badRequest("Token đặt lại mật khẩu đã hết hạn. Vui lòng thực hiện lại từ đầu.");
        }

        User user = resetToken.getUser();
        if (user == null) {
            throw ApiException.badRequest("Token đặt lại mật khẩu không hợp lệ");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userDao.save(user);
        passwordResetTokenDao.markUsed(resetToken);
        refreshTokenDao.deleteByUser(user);

        return new MessageResponse("Đặt lại mật khẩu thành công. Vui lòng đăng nhập.");
    }

    private void sendPasswordResetOtp(User user) {
        passwordResetTokenDao.deleteByUser(user);

        String otp = generateOtp();
        Instant expiresAt = Instant.now().plus(passwordResetOtpExpiration, ChronoUnit.SECONDS);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setOtpHash(passwordEncoder.encode(otp));
        resetToken.setExpiresAt(LocalDateTime.ofInstant(expiresAt, APP_ZONE));
        resetToken.setUsed(false);
        resetToken.setAttempts(0);
        passwordResetTokenDao.save(resetToken);

        int validityMinutes = (int) Math.max(1, passwordResetOtpExpiration / 60);
        String toEmail = user.getEmail();
        String fullName = user.getFullName();
        runAfterCommit(() -> emailService.sendPasswordResetOtpEmail(toEmail, fullName, otp, validityMinutes));
    }

    private String generateOtp() {
        int value = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    private void sendVerificationEmail(User user) {
        emailVerificationTokenDao.deleteByUser(user);

        Instant expiresAt = Instant.now().plus(emailVerificationTokenExpiration, ChronoUnit.SECONDS);
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(generateSecureToken());
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.ofInstant(expiresAt, APP_ZONE));
        emailVerificationTokenDao.save(token);

        String verificationUrl = appProperties.emailVerificationUrl(token.getToken());
        String toEmail = user.getEmail();
        String fullName = user.getFullName();
        runAfterCommit(() -> emailService.sendVerificationEmail(toEmail, fullName, verificationUrl));
    }

    /** Queue work only after the surrounding transaction commits (same pattern as realtime publish). */
    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    private LoginResponse buildTokenResponse(Authentication authentication, User user) {
        Instant now = Instant.now();
        Instant accessExpiresAt = now.plus(accessTokenExpiration, ChronoUnit.SECONDS);
        Instant refreshExpiresAt = now.plus(refreshTokenExpiration, ChronoUnit.SECONDS);

        String accessToken = generateAccessToken(authentication, now, accessExpiresAt);
        String refreshTokenValue = createRefreshToken(user, refreshExpiresAt);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshTokenValue);
        response.setTokenType("Bearer");
        response.setExpiresIn(accessTokenExpiration);
        response.setExpiresAt(accessExpiresAt);
        response.setRefreshExpiresIn(refreshTokenExpiration);
        response.setRefreshExpiresAt(refreshExpiresAt);
        return response;
    }

    private String createRefreshToken(User user, Instant expiresAt) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(generateSecureToken());
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.ofInstant(expiresAt, APP_ZONE));
        refreshTokenDao.save(refreshToken);
        return refreshToken.getToken();
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String resolveAvatar(MultipartFile avatar) {
        if (avatar != null && !avatar.isEmpty()) {
            return cloudinaryService.uploadAvatar(avatar);
        }
        return defaultAvatarUrl;
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateAccessToken(Authentication authentication, Instant issuedAt, Instant expiresAt) {
        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(authentication.getName())
                .claim("scope", scope)
                .claim("token_type", "access")
                .build();

        JwsHeader jwsHeader = JwsHeader.with(SecurityUtil.JWT_ALGORITHM).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }
}
