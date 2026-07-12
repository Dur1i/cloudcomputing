package com.websocial.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.websocial.models.User;
import com.websocial.models.Friendship;
import com.websocial.repositories.FriendshipRepository;
import com.websocial.repositories.PostLikeRepository;
import com.websocial.repositories.PostRepository;
import com.websocial.repositories.UserRepository;
import com.websocial.services.S3StorageService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
// Import thư viện mã hóa
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List; 

@Controller
public class UserController {

    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private PostLikeRepository postLikeRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private S3StorageService s3StorageService;

    private final String SECRET_KEY = "TCSocial#Secure#Key#2026!Pentest^&^";

    private User getLoggedInUser(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("auth_token".equals(cookie.getName())) {
                    try {
                        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
                        JWTVerifier verifier = JWT.require(algorithm).withIssuer("TC").build();
                        DecodedJWT jwt = verifier.verify(cookie.getValue());
                        return userRepository.findByUsername(jwt.getClaim("username").asString());
                    } catch (Exception e) { return null; }
                }
            }
        }
        return null;
    }

    @GetMapping("/profile/{username}")
    public String profilePage(@PathVariable String username, HttpServletRequest request, Model model) {
        User currentUser = getLoggedInUser(request);
        if (currentUser == null) return "redirect:/login"; 

        User profileUser = userRepository.findByUsername(username);
        if (profileUser == null) return "redirect:/";

        long count = userRepository.countPostsByUserId(profileUser.getId());
        long followingCount = friendshipRepository.countByRequesterAndStatus(profileUser, "ACCEPTED");
        long followerCount = friendshipRepository.countByReceiverAndStatus(profileUser, "ACCEPTED");

        // ĐÃ FIX: Hứng bằng List thay vì đối tượng đơn lẻ
        boolean isFollowing = false;
        if (!currentUser.getId().equals(profileUser.getId())) {
            List<Friendship> checks = friendshipRepository.findByRequesterAndReceiver(currentUser, profileUser);
            if (checks != null && !checks.isEmpty()) {
                if ("ACCEPTED".equals(checks.get(0).getStatus())) {
                    isFollowing = true;
                }
            }
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("profileUser", profileUser);
        model.addAttribute("postCount", count); 
        model.addAttribute("followingCount", followingCount);
        model.addAttribute("followerCount", followerCount);
        model.addAttribute("isFollowing", isFollowing);
        model.addAttribute("posts", postRepository.findByUserOrderByCreatedAtDesc(profileUser));
        model.addAttribute("likedPostIds", postLikeRepository.findByUserId(currentUser.getId()).stream()
                .map(like -> like.getPost().getId())
                .collect(java.util.stream.Collectors.toSet()));
        
        return "profile";
    }

    // API FOLLOW / UNFOLLOW (AJAX)
    @PostMapping("/api/users/{userId}/toggle-follow")
    @ResponseBody
    public String toggleFollow(@PathVariable Long userId, HttpServletRequest request) {
        User currentUser = getLoggedInUser(request);
        User targetUser = userRepository.findById(userId).orElse(null);

        if (currentUser != null && targetUser != null && !currentUser.getId().equals(userId)) {
            // ĐÃ FIX: Hứng bằng List
            List<Friendship> existing = friendshipRepository.findByRequesterAndReceiver(currentUser, targetUser);
            if (existing != null && !existing.isEmpty()) {
                friendshipRepository.deleteAll(existing);
                messagingTemplate.convertAndSend("/topic/social", java.util.Map.of(
                        "type", "follow_updated",
                        "actorId", currentUser.getId(),
                        "targetId", targetUser.getId(),
                        "status", "unfollowed",
                        "actor", toUserPayload(currentUser),
                        "target", toUserPayload(targetUser)
                ));
                return "unfollowed";
            } else {
                Friendship f = new Friendship();
                f.setRequester(currentUser);
                f.setReceiver(targetUser);
                f.setStatus("ACCEPTED");
                friendshipRepository.save(f);
                messagingTemplate.convertAndSend("/topic/social", java.util.Map.of(
                        "type", "follow_updated",
                        "actorId", currentUser.getId(),
                        "targetId", targetUser.getId(),
                        "status", "followed",
                        "actor", toUserPayload(currentUser),
                        "target", toUserPayload(targetUser)
                ));
                return "followed";
            }
        }
        return "error";
    }

    private java.util.Map<String, Object> toUserPayload(User user) {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("id", user.getId());
        payload.put("fullName", user.getFullName() != null ? user.getFullName() : user.getUsername());
        payload.put("username", user.getUsername());
        payload.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "https://abs.twimg.com/sticky/default_profile_images/default_profile_400x400.png");
        return payload;
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 HttpServletRequest request) {
        User currentUser = getLoggedInUser(request);
        
        if (currentUser != null) {
            // Khởi tạo bộ mã hóa mật khẩu
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            
            // VÁ LỖI: Kiểm tra mật khẩu cũ đã băm
            if (passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
                
                // VÁ LỖI: Băm mật khẩu mới trước khi lưu vào database
                currentUser.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(currentUser);
                
                return "redirect:/profile/" + currentUser.getUsername() + "?pw=success";
            } else {
                return "redirect:/profile/" + currentUser.getUsername() + "?pw=error";
            }
        }
        return "redirect:/login";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam("bio") String bio,
                                @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                HttpServletRequest request) {
        User currentUser = getLoggedInUser(request);
        if (currentUser != null) {
            currentUser.setBio(bio);
            if (avatarFile != null && !avatarFile.isEmpty()) {
                try {
                    currentUser.setAvatarUrl(s3StorageService.upload(avatarFile, "avatars"));
                } catch (Exception e) { e.printStackTrace(); }
            }
            userRepository.save(currentUser);
        }
        return "redirect:/profile/" + (currentUser != null ? currentUser.getUsername() : "");
    }
}
