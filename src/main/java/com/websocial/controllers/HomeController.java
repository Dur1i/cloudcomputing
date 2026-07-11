package com.websocial.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.websocial.models.Post;
import com.websocial.models.Story;
import com.websocial.models.User;
import com.websocial.repositories.FriendshipRepository;
import com.websocial.repositories.PostLikeRepository;
import com.websocial.repositories.PostRepository;
import com.websocial.repositories.StoryRepository;
import com.websocial.repositories.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate; 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    @Autowired private PostRepository postRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private StoryRepository storyRepository;
    @Autowired private PostLikeRepository postLikeRepository;
    
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
                    } catch (Exception e) { return null; }
                }
            }
        }
        return null;
    }

    @GetMapping("/")
    public String home(HttpServletRequest request, Model model) {
        User currentUser = getLoggedInUser(request);
        if (currentUser == null) return "redirect:/login";

        java.util.List<Post> posts = postRepository.findAll();
        posts.sort((p1, p2) -> p2.getId().compareTo(p1.getId()));

        LinkedHashMap<Long, User> contactMap = new LinkedHashMap<>();
        friendshipRepository.findAllFriends(currentUser).forEach(friendship -> {
            User friend = friendship.getRequester().getId().equals(currentUser.getId())
                    ? friendship.getReceiver()
                    : friendship.getRequester();
            if (friend != null && !"ADMIN".equals(friend.getRole())) {
                contactMap.putIfAbsent(friend.getId(), friend);
            }
        });
        List<User> contacts = contactMap.values().stream().toList();
        Set<Long> contactIds = contactMap.keySet();

        List<Story> activeStories = storyRepository.findByExpiresAtAfterOrderByCreatedAtDesc(new java.util.Date()).stream()
                .filter(story -> story.getUser().getId().equals(currentUser.getId()) || contactIds.contains(story.getUser().getId()))
                .toList();

        List<User> suggestedUsers = userRepository.findAll().stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .filter(user -> !"ADMIN".equals(user.getRole()))
                .filter(user -> !contactIds.contains(user.getId()))
                .limit(4)
                .toList();

        Set<Long> likedPostIds = postLikeRepository.findByUserId(currentUser.getId()).stream()
                .map(like -> like.getPost().getId())
                .collect(Collectors.toSet());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("posts", posts);
        model.addAttribute("contacts", contacts);
        model.addAttribute("activeStories", activeStories);
        model.addAttribute("suggestedUsers", suggestedUsers);
        model.addAttribute("likedPostIds", likedPostIds);
        return "index";
    }

    // ==============================================================================
    // ĐÃ VÁ LỖI BẢO MẬT: BÀI ĐĂNG RỖNG VÀ STORED XSS (Phiên bản KHÔNG JSESSIONID)
    // ==============================================================================
    @PostMapping("/create-post")
    public String createPost(@RequestParam(value = "content", required = false) String content,
                             @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                             HttpServletRequest request) { 
        
        User currentUser = getLoggedInUser(request);
        if (currentUser == null) return "redirect:/login";

        // TẤM KHIÊN 1: VÁ LỖI LOGIC BÀI ĐĂNG RỖNG (OTG-BUSLOGIC-001)
        boolean isContentEmpty = (content == null || content.trim().isEmpty());
        boolean isImageEmpty = (imageFile == null || imageFile.isEmpty());
        
        if (isContentEmpty && isImageEmpty) {
            // Truyền mã lỗi trực tiếp qua URL, không dùng Session
            return "redirect:/?error=empty_post"; 
        }

        // TẤM KHIÊN 2: VÁ LỖI HTML INJECTION (OTG-CLIENT-003) & URL REDIRECT (OTG-CLIENT-004)
        if (!isContentEmpty) {
            content = content.replace("<", "&lt;")
                             .replace(">", "&gt;")
                             .replace("\"", "&quot;")
                             .replace("'", "&#x27;");
        }

        Post post = new Post();
        post.setUser(currentUser);
        post.setContent(content); 

        // TẤM KHIÊN 3: VÁ LỖI CODE INJECTION (OTG-INPVAL-012) & UPLOAD FILE LẠ (OTG-BUSLOGIC-008)
        if (!isImageEmpty) {
            String originalFileName = imageFile.getOriginalFilename();
            if (originalFileName != null) {
                String lowerCaseName = originalFileName.toLowerCase();
                
                boolean allowedImage = lowerCaseName.endsWith(".jpg")
                        || lowerCaseName.endsWith(".jpeg")
                        || lowerCaseName.endsWith(".png")
                        || lowerCaseName.endsWith(".gif")
                        || lowerCaseName.endsWith(".webp");
                boolean allowedVideo = lowerCaseName.endsWith(".mp4")
                        || lowerCaseName.endsWith(".webm")
                        || lowerCaseName.endsWith(".ogg");

                if (!allowedImage && !allowedVideo) {
                    
                    return "redirect:/?error=invalid_format"; // Truyền mã lỗi qua URL
                }
                
                // Kiểm tra MIME Type 
                String mimeType = imageFile.getContentType();
                if (mimeType == null || !(mimeType.startsWith("image/") || mimeType.startsWith("video/"))) {
                    return "redirect:/?error=not_image"; // Truyền mã lỗi qua URL
                }

                try {
                    String uploadsDir = System.getProperty("user.dir") + "/uploads/";
                    java.io.File dir = new java.io.File(uploadsDir);
                    if (!dir.exists()) dir.mkdirs();

                    String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
                    String newFileName = java.util.UUID.randomUUID().toString() + fileExtension;
                    
                    java.io.File destinationFile = new java.io.File(uploadsDir + newFileName);
                    imageFile.transferTo(destinationFile);
                    post.setMediaUrl("/uploads/" + newFileName);
                } catch (java.io.IOException e) { e.printStackTrace(); }
            }
        }

        postRepository.save(post);

        // === BẮN DỮ LIỆU REAL-TIME LÊN KÊNH /TOPIC/POSTS ===
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", post.getId());
        payload.put("content", post.getContent());
        payload.put("fullName", currentUser.getFullName());
        payload.put("username", currentUser.getUsername());
        payload.put("avatarUrl", currentUser.getAvatarUrl() != null ? currentUser.getAvatarUrl() : "https://abs.twimg.com/sticky/default_profile_images/default_profile_400x400.png");
        payload.put("mediaUrl", post.getMediaUrl());
        
        messagingTemplate.convertAndSend("/topic/posts", payload);

        return "redirect:/?posted=success";
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("auth_token", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/login";
    }
}
