package com.websocial.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.websocial.models.Comment;
import com.websocial.models.Post;
import com.websocial.models.PostLike;
import com.websocial.models.Repost;
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
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class InteractionController {

    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private PostLikeRepository postLikeRepository;
    @Autowired private RepostRepository repostRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate;

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

    @PostMapping("/api/like/{postId}")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long postId, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        User user = getLoggedInUser(request);
        if (user == null) {
            response.put("status", "error");
            return ResponseEntity.status(401).body(response);
        }

        if (postLikeRepository.existsByUserIdAndPostId(user.getId(), postId)) {
            postLikeRepository.deleteByUserIdAndPostId(user.getId(), postId);
            response.put("status", "unliked");
            response.put("liked", false);
        } else {
            Post post = postRepository.findById(postId).orElse(null);
            if (post == null) {
                response.put("status", "error");
                return ResponseEntity.badRequest().body(response);
            }

            PostLike like = new PostLike();
            like.setUser(user);
            like.setPost(post);
            postLikeRepository.save(like);
            response.put("status", "liked");
            response.put("liked", true);
        }

        response.put("likeCount", postLikeRepository.countByPostId(postId));
        response.put("postId", postId);
        response.put("actorId", user.getId());
        messagingTemplate.convertAndSend("/topic/social", Map.of(
                "type", "like_updated",
                "postId", postId,
                "actorId", user.getId(),
                "liked", response.get("liked"),
                "likeCount", response.get("likeCount")
        ));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/repost/{postId}")
    @ResponseBody
    public String repost(@PathVariable Long postId, HttpServletRequest request) {
        User user = getLoggedInUser(request);
        Optional<Post> post = postRepository.findById(postId);
        if (user != null && post.isPresent()) {
            Repost repost = new Repost();
            repost.setUser(user);
            repost.setPost(post.get());
            repostRepository.save(repost);
            return "reposted";
        }
        return "error";
    }

    @PostMapping("/post/comment")
    public String addComment(@RequestParam("postId") Long postId,
                             @RequestParam("content") String content,
                             HttpServletRequest request) {
        User user = getLoggedInUser(request);
        Optional<Post> post = postRepository.findById(postId);

        if (user != null && post.isPresent() && content != null && !content.trim().isEmpty()) {
            Comment comment = new Comment();
            comment.setUser(user);
            comment.setPost(post.get());
            comment.setContent(sanitize(content));
            commentRepository.save(comment);
            messagingTemplate.convertAndSend("/topic/social", Map.of(
                    "type", "comment_created",
                    "postId", postId,
                    "actorId", user.getId(),
                    "comment", toCommentPayload(comment),
                    "commentCount", commentRepository.countByPostId(postId)
            ));
        }

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    @PostMapping("/api/comment")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addCommentApi(@RequestParam("postId") Long postId,
                                                             @RequestParam("content") String content,
                                                             HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        User user = getLoggedInUser(request);
        Optional<Post> post = postRepository.findById(postId);

        if (user == null) {
            response.put("status", "error");
            return ResponseEntity.status(401).body(response);
        }
        if (post.isEmpty() || content == null || content.trim().isEmpty()) {
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
        }

        Comment comment = new Comment();
        comment.setUser(user);
        comment.setPost(post.get());
        comment.setContent(sanitize(content));
        commentRepository.save(comment);

        response.put("status", "created");
        response.put("comment", toCommentPayload(comment));
        response.put("commentCount", commentRepository.countByPostId(postId));
        messagingTemplate.convertAndSend("/topic/social", Map.of(
                "type", "comment_created",
                "postId", postId,
                "actorId", user.getId(),
                "comment", response.get("comment"),
                "commentCount", response.get("commentCount")
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/comments/{postId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getComments(@PathVariable Long postId) {
        Optional<Post> post = postRepository.findById(postId);
        if (post.isEmpty()) return ResponseEntity.badRequest().body(List.of());

        List<Map<String, Object>> comments = commentRepository.findByPostOrderByCreatedAtAsc(post.get()).stream()
                .map(this::toCommentPayload)
                .toList();
        return ResponseEntity.ok(comments);
    }

    private Map<String, Object> toCommentPayload(Comment comment) {
        User user = comment.getUser();
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", comment.getId());
        payload.put("content", comment.getContent());
        payload.put("fullName", user.getFullName());
        payload.put("username", user.getUsername());
        payload.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "https://abs.twimg.com/sticky/default_profile_images/default_profile_400x400.png");
        return payload;
    }

    private String sanitize(String value) {
        return value.trim()
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
