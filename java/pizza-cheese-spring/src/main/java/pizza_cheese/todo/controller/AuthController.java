package pizza_cheese.todo.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import pizza_cheese.todo.dto.request.ForgotPasswordRequest;
import pizza_cheese.todo.dto.request.LoginRequest;
import pizza_cheese.todo.dto.request.RefreshTokenRequest;
import pizza_cheese.todo.dto.request.RegisterRequest;
import pizza_cheese.todo.dto.request.ResendVerificationRequest;
import pizza_cheese.todo.dto.request.ResetPasswordRequest;
import pizza_cheese.todo.dto.request.VerifyResetOtpRequest;
import pizza_cheese.todo.dto.response.ForgotPasswordResponse;
import pizza_cheese.todo.dto.response.LoginResponse;
import pizza_cheese.todo.dto.response.MessageResponse;
import pizza_cheese.todo.dto.response.RegisterPendingResponse;
import pizza_cheese.todo.dto.response.RestResponse;
import pizza_cheese.todo.dto.response.UserProfileResponse;
import pizza_cheese.todo.dto.response.VerifyResetOtpResponse;
import pizza_cheese.todo.service.AuthService;

@Tag(name = "Auth", description = "Đăng ký, đăng nhập và quản lý phiên")

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Đăng ký tài khoản mới (JSON)",
            description = "Tạo tài khoản pending và gửi email xác thực. Chưa trả JWT cho đến khi verify.")
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RestResponse<RegisterPendingResponse>> registerJson(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(RestResponse.success(authService.register(request, null)));
    }

    @Operation(
            summary = "Đăng ký tài khoản mới (multipart + avatar)",
            description = "Gửi multipart/form-data gồm part `request` (JSON) và file `avatar` (tuỳ chọn).")
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RestResponse<RegisterPendingResponse>> registerMultipart(
            @Valid @RequestPart("request") RegisterRequest request,
            @RequestPart(value = "avatar", required = false) @Schema(type = "string", format = "binary") MultipartFile avatar) {
        return ResponseEntity.ok(RestResponse.success(authService.register(request, avatar)));
    }

    @Operation(summary = "Xác thực email bằng token trong link")
    @GetMapping("/verify-email")
    public ResponseEntity<RestResponse<MessageResponse>> verifyEmail(@RequestParam("token") String token) {
        return ResponseEntity.ok(RestResponse.success(authService.verifyEmail(token)));
    }

    @Operation(summary = "Gửi lại email xác thực")
    @PostMapping("/resend-verification")
    public ResponseEntity<RestResponse<MessageResponse>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        return ResponseEntity.ok(RestResponse.success(authService.resendVerification(request.getEmail())));
    }

    @Operation(
            summary = "Quên mật khẩu — gửi OTP qua email",
            description = "Luôn trả về thông báo chung để không lộ email có trong hệ thống hay không.")
    @PostMapping("/forgot-password")
    public ResponseEntity<RestResponse<ForgotPasswordResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(RestResponse.success(authService.forgotPassword(request.getEmail())));
    }

    @Operation(summary = "Xác thực OTP đặt lại mật khẩu")
    @PostMapping("/verify-reset-otp")
    public ResponseEntity<RestResponse<VerifyResetOtpResponse>> verifyResetOtp(
            @Valid @RequestBody VerifyResetOtpRequest request) {
        return ResponseEntity.ok(RestResponse.success(
                authService.verifyResetOtp(request.getEmail(), request.getOtp())));
    }

    @Operation(summary = "Đặt mật khẩu mới bằng reset token")
    @PostMapping("/reset-password")
    public ResponseEntity<RestResponse<MessageResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(RestResponse.success(
                authService.resetPassword(request.getResetToken(), request.getNewPassword())));
    }

    @Operation(summary = "Đăng nhập bằng email hoặc tên đăng nhập")
    @PostMapping("/login")
    public ResponseEntity<RestResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.login(request);
        return ResponseEntity.ok(RestResponse.success(loginResponse));
    }

    @Operation(summary = "Đăng xuất")
    @PostMapping("/logout")
    public ResponseEntity<RestResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(RestResponse.success(null));
    }

    @Operation(summary = "Làm mới access token")
    @PostMapping("/refresh")
    public ResponseEntity<RestResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse tokenResponse = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(RestResponse.success(tokenResponse));
    }

    @Operation(summary = "Lấy thông tin tài khoản hiện tại")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/account")
    public ResponseEntity<RestResponse<UserProfileResponse>> getAccount(@AuthenticationPrincipal Jwt jwt) {
        UserProfileResponse profile = authService.getProfile(jwt.getSubject());
        return ResponseEntity.ok(RestResponse.success(profile));
    }
}
