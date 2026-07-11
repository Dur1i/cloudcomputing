package com.websocial.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.websocial.models.Post;
import com.websocial.models.User;
import com.websocial.repositories.CommentRepository;
import com.websocial.repositories.PostLikeRepository;
import com.websocial.repositories.PostRepository;
import com.websocial.repositories.RepostRepository;
import com.websocial.repositories.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostApiController {

    @Autowired private PostRepository postRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private PostLikeRepository postLikeRepository;
    @Autowired private RepostRepository repostRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    private final String SECRET_KEY = "TCSocial#Secure#Key#2026!Pentest^&^";

    // Hàm hỗ trợ: Trích xuất thông tin User đang đăng nhập từ Cookie (JWT)
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

    @GetMapping
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getPosts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpServletRequest request) {
        User currentUser = getLoggedInUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(401).body(null);
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("id").descending());
        org.springframework.data.domain.Page<Post> postsPage = postRepository.findAll(pageable);

        java.util.Set<Long> likedPostIds = postLikeRepository.findByUserId(currentUser.getId()).stream()
                .map(like -> like.getPost().getId())
                .collect(java.util.stream.Collectors.toSet());

        java.util.List<java.util.Map<String, Object>> result = postsPage.getContent().stream().map(post -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", post.getId());
            map.put("content", post.getContent());
            map.put("mediaUrl", post.getMediaUrl());
            map.put("fullName", post.getUser().getFullName());
            map.put("username", post.getUser().getUsername());
            map.put("avatarUrl", post.getUser().getAvatarUrl() != null ? post.getUser().getAvatarUrl() : "https://abs.twimg.com/sticky/default_profile_images/default_profile_400x400.png");
            map.put("likeCount", post.getLikeCount());
            map.put("commentCount", post.getCommentCount());
            map.put("liked", likedPostIds.contains(post.getId()));
            map.put("userId", post.getUser().getId());
            
            java.util.List<java.util.Map<String, Object>> comments = new java.util.ArrayList<>();
            if (post.getComments() != null) {
                for (com.websocial.models.Comment cmt : post.getComments()) {
                    java.util.Map<String, Object> cmap = new java.util.HashMap<>();
                    cmap.put("id", cmt.getId());
                    cmap.put("content", cmt.getContent());
                    cmap.put("fullName", cmt.getUser().getFullName());
                    cmap.put("username", cmt.getUser().getUsername());
                    cmap.put("avatarUrl", cmt.getUser().getAvatarUrl() != null ? cmt.getUser().getAvatarUrl() : "https://abs.twimg.com/sticky/default_profile_images/default_profile_400x400.png");
                    comments.add(cmap);
                }
            }
            map.put("comments", comments);
            return map;
        }).collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/delete/{id}")
    @Transactional // Rất quan trọng để không bị lỗi khi xóa Comment/Like
    public ResponseEntity<String> deletePost(@PathVariable Long id, HttpServletRequest request) {
        // 1. Biết người đang yêu cầu là ai
        User currentUser = getLoggedInUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(401).body("Vui lòng đăng nhập!");
        }

        // 2. Lấy bài viết ra để kiểm tra
        Post post = postRepository.findById(id).orElse(null);
        if (post != null) {
            
            // 3. CHỐT CHẶN BẢO MẬT: Chỉ cho xóa nếu là Tác Giả hoặc ADMIN
            if (post.getUser().getId().equals(currentUser.getId()) || "ADMIN".equals(currentUser.getRole())) {
                
                // Dọn dẹp "mạng nhện" trước khi xóa bài để tránh lỗi SQL
                commentRepository.deleteByPost(post);
                postLikeRepository.deleteByPost(post);
                repostRepository.deleteByPost(post);
                
                // Trảm!
                postRepository.delete(post);
                messagingTemplate.convertAndSend("/topic/social", java.util.Map.of(
                        "type", "post_deleted",
                        "postId", id,
                        "actorId", currentUser.getId()
                ));
                return ResponseEntity.ok("Đã xóa bài viết thành công!");
                
            } else {
                // Bắt quả tang cố tình xóa bài người khác
                return ResponseEntity.status(403).body("Lỗi bảo mật: Bạn không có quyền xóa bài viết này!");
            }
        }
        return ResponseEntity.badRequest().body("Bài viết không tồn tại!");
    }
}
