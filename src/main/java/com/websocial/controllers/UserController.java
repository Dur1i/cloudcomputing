package com.websocial.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.websocial.models.Friendship;
import com.websocial.models.User;
import com.websocial.repositories.FriendshipRepository;
import com.websocial.repositories.PostLikeRepository;
import com.websocial.repositories.PostRepository;
import com.websocial.repositories.UserRepository;
import com.websocial.services.S3StorageService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (!"auth_token".equals(cookie.getName())) continue;
            try {
                Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
                JWTVerifier verifier = JWT.require(algorithm).withIssuer("TC").build();
                DecodedJWT jwt = verifier.verify(cookie.getValue());
                return userRepository.findByUsername(jwt.getClaim("username").asString());
            } catch (Exception e) {
                return null;
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

        boolean isFollowing = false;
        if (!currentUser.getId().equals(profileUser.getId())) {
            List<Friendship> checks = friendshipRepository.findByRequesterAndReceiver(currentUser, profileUser);
            isFollowing = checks != null && !checks.isEmpty() && "ACCEPTED".equals(checks.get(0).getStatus());
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
                .collect(Collectors.toSet()));

        return "profile";
    }

    @PostMapping("/api/users/{userId}/toggle-follow")
    @ResponseBody
    public String toggleFollow(@PathVariable Long userId, HttpServletRequest request) {
        User currentUser = getLoggedInUser(request);
        User targetUser = userRepository.findById(userId).orElse(null);

        if (currentUser == null || targetUser == null || currentUser.getId().equals(userId)) {
            return "error";
        }

        List<Friendship> existing = friendshipRepository.findByRequesterAndReceiver(currentUser, targetUser);
        if (existing != null && !existing.isEmpty()) {
            friendshipRepository.deleteAll(existing);
            messagingTemplate.convertAndSend("/topic/social", Map.of(
                    "type", "follow_updated",
                    "actorId", currentUser.getId(),
                    "targetId", targetUser.getId(),
                    "status", "unfollowed",
                    "actor", toUserPayload(currentUser),
                    "target", toUserPayload(targetUser)
            ));
            return "unfollowed";
        }

        Friendship friendship = new Friendship();
        friendship.setRequester(currentUser);
        friendship.setReceiver(targetUser);
        friendship.setStatus("ACCEPTED");
        friendshipRepository.save(friendship);
        messagingTemplate.convertAndSend("/topic/social", Map.of(
                "type", "follow_updated",
                "actorId", currentUser.getId(),
                "targetId", targetUser.getId(),
                "status", "followed",
                "actor", toUserPayload(currentUser),
                "target", toUserPayload(targetUser)
        ));
        return "followed";
    }

    private Map<String, Object> toUserPayload(User user) {
        Map<String, Object> payload = new HashMap<>();
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
        if (currentUser == null) return "redirect:/login";

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String storedPassword = currentUser.getPassword();
        boolean matchesHashedPassword = storedPassword != null && passwordEncoder.matches(oldPassword, storedPassword);
        boolean matchesLegacyPlainPassword = storedPassword != null && storedPassword.equals(oldPassword);

        if (!matchesHashedPassword && !matchesLegacyPlainPassword) {
            return "redirect:/profile/" + currentUser.getUsername() + "?pw=error";
        }

        currentUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(currentUser);
        return "redirect:/profile/" + currentUser.getUsername() + "?pw=success";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam(value = "bio", required = false) String bio,
                                @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                HttpServletRequest request) {
        User currentUser = getLoggedInUser(request);
        if (currentUser == null) return "redirect:/login";

        try {
            currentUser.setBio(bio != null ? bio.trim() : "");
            if (avatarFile != null && !avatarFile.isEmpty()) {
                currentUser.setAvatarUrl(s3StorageService.upload(avatarFile, "avatars"));
            }
            userRepository.save(currentUser);
            return "redirect:/profile/" + currentUser.getUsername() + "?profile=success";
        } catch (Exception e) {
            return "redirect:/profile/" + currentUser.getUsername() + "?profile=error";
        }
    }
}
