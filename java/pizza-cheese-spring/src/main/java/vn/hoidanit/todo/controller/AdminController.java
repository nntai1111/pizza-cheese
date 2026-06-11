package vn.hoidanit.todo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.hoidanit.todo.dto.response.RestResponse;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<String>> dashboard() {
        return ResponseEntity.ok(RestResponse.success("Chào mừng Admin!"));
    }
}
