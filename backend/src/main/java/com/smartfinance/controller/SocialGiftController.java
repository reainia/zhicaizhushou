package com.smartfinance.controller;

import com.smartfinance.dto.SocialGiftRequest;
import com.smartfinance.entity.SocialGift;
import com.smartfinance.entity.User;
import com.smartfinance.repository.SocialGiftRepository;
import com.smartfinance.repository.UserRepository;
import com.smartfinance.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/social-gifts")
public class SocialGiftController {

    @Autowired
    private SocialGiftRepository socialGiftRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private User getUserFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未提供令牌");
        }
        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @GetMapping
    public ResponseEntity<?> listGifts(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        try {
            User user = getUserFromAuthHeader(authHeader);
            List<SocialGift> gifts;

            if (year != null && month != null) {
                LocalDate start = LocalDate.of(year, month, 1);
                LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
                gifts = socialGiftRepository.findByUserIdAndEventDateBetween(user.getId(), start, end);
            } else {
                gifts = socialGiftRepository.findByUserIdOrderByEventDateDesc(user.getId());
            }

            return ResponseEntity.ok(gifts);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        try {
            User user = getUserFromAuthHeader(authHeader);
            LocalDate now = LocalDate.now();
            int y = year != null ? year : now.getYear();
            int m = month != null ? month : now.getMonthValue();
            LocalDate start = LocalDate.of(y, m, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

            BigDecimal totalGiven = socialGiftRepository.sumByUserIdAndDirectionAndDateBetween(user.getId(), "GIVE", start, end);
            BigDecimal totalReceived = socialGiftRepository.sumByUserIdAndDirectionAndDateBetween(user.getId(), "RECEIVE", start, end);

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalGiven", totalGiven);
            summary.put("totalReceived", totalReceived);
            summary.put("netAmount", totalReceived.subtract(totalGiven));

            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createGift(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SocialGiftRequest request) {
        try {
            User user = getUserFromAuthHeader(authHeader);

            SocialGift gift = new SocialGift();
            gift.setUserId(user.getId());
            gift.setEventType(request.getEventType());
            gift.setPersonName(request.getPersonName());
            gift.setAmount(request.getAmount());
            gift.setDirection(request.getDirection());
            gift.setEventDate(request.getEventDate());
            gift.setDescription(request.getDescription());
            gift.setCreatedAt(LocalDateTime.now());

            socialGiftRepository.save(gift);
            return ResponseEntity.ok(gift);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateGift(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody SocialGiftRequest request) {
        try {
            User user = getUserFromAuthHeader(authHeader);

            SocialGift gift = socialGiftRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("人情记录不存在"));

            if (!gift.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body("无权修改此记录");
            }

            gift.setEventType(request.getEventType());
            gift.setPersonName(request.getPersonName());
            gift.setAmount(request.getAmount());
            gift.setDirection(request.getDirection());
            gift.setEventDate(request.getEventDate());
            gift.setDescription(request.getDescription());

            socialGiftRepository.save(gift);
            return ResponseEntity.ok(gift);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGift(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        try {
            User user = getUserFromAuthHeader(authHeader);

            SocialGift gift = socialGiftRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("人情记录不存在"));

            if (!gift.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body("无权删除此记录");
            }

            socialGiftRepository.delete(gift);
            return ResponseEntity.ok("删除成功");
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}
