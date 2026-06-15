package pizza_cheese.todo.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
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

import pizza_cheese.todo.domain.RefreshToken;
import pizza_cheese.todo.domain.Role;
import pizza_cheese.todo.domain.User;
import pizza_cheese.todo.dto.request.LoginRequest;
import pizza_cheese.todo.dto.request.RegisterRequest;
import pizza_cheese.todo.dto.response.LoginResponse;
import pizza_cheese.todo.dto.response.UserProfileResponse;
import pizza_cheese.todo.exception.EmailAlreadyExistsException;
import pizza_cheese.todo.exception.InvalidRefreshTokenException;
import pizza_cheese.todo.dao.RefreshTokenDao;
import pizza_cheese.todo.dao.UserDao;
import pizza_cheese.todo.util.SecurityUtil;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final PasswordEncoder passwordEncoder;
    private final UserDao userDao;
    private final RefreshTokenDao refreshTokenDao;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            PasswordEncoder passwordEncoder,
            UserDao userDao,
            RefreshTokenDao refreshTokenDao,
            @Value("${app.jwt.access-token-validity-in-seconds}") long accessTokenExpiration,
            @Value("${app.jwt.refresh-token-validity-in-seconds}") long refreshTokenExpiration) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.passwordEncoder = passwordEncoder;
        this.userDao = userDao;
        this.refreshTokenDao = refreshTokenDao;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userDao.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email đã được sử dụng");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(resolveUniqueUsername(request.getEmail()));
        user.setFullName(request.getName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(Role.CUSTOMER));
        userDao.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(request.getEmail());
        loginRequest.setPassword(request.getPassword());
        return login(loginRequest);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenDao.findByToken(refreshTokenValue)
                .ifPresent(refreshTokenDao::delete);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword());

        Authentication authentication = authenticationManager.authenticate(authToken);

        User user = userDao.findByEmail(request.getEmail())
                .orElseThrow();

        refreshTokenDao.deleteByUser(user);

        LoginResponse response = buildTokenResponse(authentication, user);
        response.setUser(UserProfileResponse.from(user));
        return response;
    }

    @Transactional
    public LoginResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenDao.findByToken(refreshTokenValue)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token không hợp lệ"));

        if (refreshToken.isExpired()) {
            refreshTokenDao.delete(refreshToken);
            throw new InvalidRefreshTokenException("Refresh token đã hết hạn");
        }

        User user = refreshToken.getUser();
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
        refreshToken.setToken(generateRefreshTokenValue());
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(expiresAt);
        refreshTokenDao.save(refreshToken);
        return refreshToken.getToken();
    }

    private String generateRefreshTokenValue() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String resolveUniqueUsername(String email) {
        String base = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String username = base;
        int suffix = 1;
        while (userDao.existsByUsername(username)) {
            username = base + suffix++;
        }
        return username;
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
