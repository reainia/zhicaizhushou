package com.smartfinance.controller;

import com.smartfinance.dto.AuthRequest;
import com.smartfinance.dto.AuthResponse;
import com.smartfinance.dto.UpdateProfileRequest;
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
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            return ResponseEntity.badRequest().body("邮箱不能为空");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("邮箱已被注册");
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // 注册成功后自动登录，返回 token
        String token = jwtUtil.generateToken(user.getEmail());
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getAvatarUrl(),
            user.getCreatedAt()
        );
        return ResponseEntity.ok(new AuthResponse(token, userInfo));
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
            user.getAvatarUrl(),
            user.getCreatedAt()
        );
        
        return ResponseEntity.ok(new AuthResponse(token, userInfo));
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("未提供令牌");
        }
        
        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("avatarUrl", user.getAvatarUrl());
        profile.put("createdAt", user.getCreatedAt());
        
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateProfileRequest request) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("未提供令牌");
            }
            
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            if (request.getUsername() != null && !request.getUsername().isEmpty()) {
                user.setUsername(request.getUsername());
            }
            if (request.getEmail() != null && !request.getEmail().isEmpty()) {
                user.setEmail(request.getEmail());
            }
            user.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(user);
            
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", user.getId());
            profile.put("username", user.getUsername());
            profile.put("email", user.getEmail());
            profile.put("avatarUrl", user.getAvatarUrl());
            profile.put("createdAt", user.getCreatedAt());
            
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}