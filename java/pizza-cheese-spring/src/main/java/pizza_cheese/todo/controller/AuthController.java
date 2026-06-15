package pizza_cheese.todo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pizza_cheese.todo.dto.request.LoginRequest;
import pizza_cheese.todo.dto.request.RefreshTokenRequest;
import pizza_cheese.todo.dto.request.RegisterRequest;
import pizza_cheese.todo.dto.response.LoginResponse;
import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.dto.response.UserProfileResponse;
import pizza_cheese.todo.service.AuthService;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RestResponse<LoginResponse>> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse loginResponse = authService.register(request);
        return ResponseEntity.ok(RestResponse.success(loginResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<RestResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.login(request);
        return ResponseEntity.ok(RestResponse.success(loginResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<RestResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(RestResponse.success(null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RestResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse tokenResponse = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(RestResponse.success(tokenResponse));
    }

    @GetMapping("/account")
    public ResponseEntity<RestResponse<UserProfileResponse>> getAccount(@AuthenticationPrincipal Jwt jwt) {
        UserProfileResponse profile = authService.getProfile(jwt.getSubject());
        return ResponseEntity.ok(RestResponse.success(profile));
    }
}
