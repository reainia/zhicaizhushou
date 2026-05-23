package com.smartfinance.controller;

import com.smartfinance.dto.AuthRequest;
import com.smartfinance.dto.AuthResponse;
import com.smartfinance.entity.User;
import com.smartfinance.repository.UserRepository;
import com.smartfinance.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body("邮箱已被注册");
        }
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok("注册成功");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        // 先尝试通过邮箱查找，如果找不到则通过用户名查找
        User user = userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> userRepository.findByUsername(request.getUsername())
                        .orElseThrow(() -> new RuntimeException("用户不存在")));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body("密码错误");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        
        // 构建用户信息
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getAvatarUrl()
        );
        
        return ResponseEntity.ok(new AuthResponse(token, userInfo));
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("未提供令牌");
        }
        
        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token); // 需要在 JwtUtil 中增加公开方法
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("avatarUrl", user.getAvatarUrl());
        
        return ResponseEntity.ok(profile);
    }
}