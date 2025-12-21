package com.nexus.chat.controller;

import com.nexus.chat.dto.AuthResponse;
import com.nexus.chat.dto.LoginRequest;
import com.nexus.chat.dto.RegisterRequest;
import com.nexus.chat.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        log.info("用户注册请求: username={}, email={}", request.getUsername(), request.getEmail());
        try {
            AuthResponse response = authService.register(request);
            log.info("用户注册成功: userId={}, username={}", response.getUserId(), response.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("用户注册失败: username={}, reason={}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("用户登录请求: usernameOrEmail={}", request.getUsernameOrEmail());
        try {
            AuthResponse response = authService.login(request);
            log.info("用户登录成功: userId={}, username={}", response.getUserId(), response.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("用户登录失败: usernameOrEmail={}, reason={}", request.getUsernameOrEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam Long userId) {
        log.info("用户登出请求: userId={}", userId);
        authService.logout(userId);
        log.info("用户登出成功: userId={}", userId);
        return ResponseEntity.ok().build();
    }

}
