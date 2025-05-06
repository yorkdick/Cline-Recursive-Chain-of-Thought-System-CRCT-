package com.rayfay.gira.controller;

import com.rayfay.gira.dto.request.LoginRequest;
import com.rayfay.gira.dto.request.RefreshTokenRequest;
import com.rayfay.gira.dto.response.LoginResponse;
import com.rayfay.gira.service.interfaces.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", HttpStatus.UNAUTHORIZED.value());
            response.put("error", "Unauthorized");
            response.put("message", e.getMessage());
            response.put("path", "/api/auth/refresh-token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}