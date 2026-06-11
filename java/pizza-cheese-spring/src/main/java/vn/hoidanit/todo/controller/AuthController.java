package vn.hoidanit.todo.controller;

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
import vn.hoidanit.todo.dto.request.LoginRequest;
import vn.hoidanit.todo.dto.request.RefreshTokenRequest;
import vn.hoidanit.todo.dto.response.LoginResponse;
import vn.hoidanit.todo.dto.response.RestResponse;
import vn.hoidanit.todo.dto.response.UserProfileResponse;
import vn.hoidanit.todo.service.AuthService;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<RestResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.login(request);
        return ResponseEntity.ok(RestResponse.success(loginResponse));
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
