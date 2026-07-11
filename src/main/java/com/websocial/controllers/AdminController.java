package com.websocial.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.websocial.models.User;
import com.websocial.repositories.PostRepository;
import com.websocial.repositories.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
// Import thêm thư viện mã hóa mật khẩu
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.websocial.repositories.ChatMessageRepository;
import com.websocial.repositories.CommentRepository;
import com.websocial.repositories.FriendshipRepository;
import com.websocial.repositories.PostLikeRepository;
import com.websocial.repositories.StoryRepository;

import com.websocial.repositories.RepostRepository;

@Controller
public class AdminController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private PostLikeRepository postLikeRepository;
    @Autowired
    private RepostRepository repostRepository;
    @Autowired
    private StoryRepository storyRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // ĐÃ VÁ LỖI (BƯỚC 2): Sử dụng Khóa bí mật siêu mạnh (256-bit) chống Hashcat
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
                    } catch (Exception e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    @GetMapping("/admin")
    public String adminDashboard(HttpServletRequest request, Model model) {
        User currentUser = getLoggedInUser(request);
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (!"ADMIN".equals(currentUser.getRole())) {
            return "redirect:/?error=unauthorized";
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("posts", postRepository.findAll());
        return "admin";
    }

    @PostMapping("/admin/deletePost/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String deletePost(@PathVariable Long id, HttpServletRequest request) {
        User currentUser = getLoggedInUser(request);
        if (currentUser == null) {
            return "redirect:/login";
        }

        // VÁ LỖI BROKEN ACCESS CONTROL: Chỉ ADMIN mới được xóa bài
        if (!"ADMIN".equals(currentUser.getRole())) {
            return "redirect:/?error=unauthorized";
        }

        com.websocial.models.Post post = postRepository.findById(id).orElse(null);
        if (post != null) {
            commentRepository.deleteByPost(post);
            postLikeRepository.deleteByPost(post);
            repostRepository.deleteByPost(post);
            postRepository.delete(post);
            messagingTemplate.convertAndSend("/topic/social", java.util.Map.of(
                    "type", "post_deleted",
                    "postId", id,
                    "actorId", currentUser.getId()
            ));
        }
        return "redirect:/admin";
    }

    // ==============================================================================
    // TÍNH NĂNG MỚI: ADMIN ĐỔI MẬT KHẨU NGƯỜI DÙNG (CẬP NHẬT)
    // ==============================================================================
    @PostMapping("/admin/changeUserPassword/{id}")
    public String changeUserPassword(@PathVariable Long id,
            @RequestParam("newPassword") String newPassword,
            HttpServletRequest request) {
        User currentUser = getLoggedInUser(request);
        if (currentUser == null) {
            return "redirect:/login";
        }

        // BẢO MẬT: Kiểm tra quyền chặt chẽ từ Database
        if (!"ADMIN".equals(currentUser.getRole())) {
            return "redirect:/?error=unauthorized";
        }

        User targetUser = userRepository.findById(id).orElse(null);
        if (targetUser != null) {
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                // Băm mật khẩu bằng BCrypt trước khi lưu
                BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                targetUser.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(targetUser);

                return "redirect:/admin?success=password-updated";
            }
            return "redirect:/admin?error=empty-password";
        }
        return "redirect:/admin?error=user-not-found";
    }

    // ==============================================================================
    // BẪY HONEYPOT ĐÃ ĐƯỢC NÂNG CẤP (VÁ LỖI GIẢ MẠO JWT)
    // ==============================================================================
    @PostMapping("/admin/deleteUser/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String deleteUser(
            @PathVariable Long id,
            HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {

        User currentUser = getLoggedInUser(request);
        if (currentUser == null) {
            return "redirect:/login";
        }

        // VÁ LỖI: Lấy Role trực tiếp từ Database (currentUser.getRole()), 
        // TUYỆT ĐỐI KHÔNG đọc Role từ gói tin JWT do người dùng gửi lên nữa!
        String realDatabaseRole = currentUser.getRole();

        // KHU VỰC TRỪNG PHẠT: Dù Hacker có dùng jwt.io sửa chữ USER thành ADMIN, 
        // biến realDatabaseRole vẫn sẽ lật tẩy bản chất thật của chúng!
        if (!"ADMIN".equals(realDatabaseRole)) {
            try {
                // Tịch thu tài sản và Xóa sổ kẻ vi phạm
                commentRepository.deleteByUser(currentUser);
                postLikeRepository.deleteByUser(currentUser);
                repostRepository.deleteByUser(currentUser);
                chatMessageRepository.deleteBySenderOrReceiver(currentUser, currentUser);
                friendshipRepository.deleteByRequesterOrReceiver(currentUser, currentUser);
                storyRepository.deleteByUser(currentUser);

                java.util.List<com.websocial.models.Post> userPosts = postRepository.findByUser(currentUser);
                for (com.websocial.models.Post post : userPosts) {
                    commentRepository.deleteByPost(post);
                    postLikeRepository.deleteByPost(post);
                    repostRepository.deleteByPost(post);
                    postRepository.delete(post);
                }
                userRepository.deleteById(currentUser.getId());
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Xóa Cookie để cưỡng chế văng ra ngoài
            Cookie jwtCookie = new Cookie("auth_token", null);
            jwtCookie.setMaxAge(0);
            jwtCookie.setPath("/");
            response.addCookie(jwtCookie);

            return "redirect:/login?error=honeypot";
        }

        // KHU VỰC BÌNH THƯỜNG: DÀNH CHO ADMIN THẬT
        if (!currentUser.getId().equals(id)) {
            User targetUser = userRepository.findById(id).orElse(null);
            if (targetUser != null) {
                try {
                    commentRepository.deleteByUser(targetUser);
                    postLikeRepository.deleteByUser(targetUser);
                    repostRepository.deleteByUser(targetUser);
                    chatMessageRepository.deleteBySenderOrReceiver(targetUser, targetUser);
                    friendshipRepository.deleteByRequesterOrReceiver(targetUser, targetUser);
                    storyRepository.deleteByUser(targetUser);

                    java.util.List<com.websocial.models.Post> targetPosts = postRepository.findByUser(targetUser);
                    for (com.websocial.models.Post post : targetPosts) {
                        commentRepository.deleteByPost(post);
                        postLikeRepository.deleteByPost(post);
                        repostRepository.deleteByPost(post);
                        postRepository.delete(post);
                    }
                    userRepository.deleteById(id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return "redirect:/admin";
    }
}
