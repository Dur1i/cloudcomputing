package com.websocial.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.websocial.models.Story;
import com.websocial.models.User;
import com.websocial.repositories.StoryRepository;
import com.websocial.repositories.UserRepository;
import com.websocial.services.S3StorageService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

@Controller
public class StoryController {

    @Autowired private UserRepository userRepository;
    @Autowired private StoryRepository storyRepository;
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
                    } catch (Exception e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    @PostMapping("/stories/create")
    public String createStory(@RequestParam(value = "caption", required = false) String caption,
                              @RequestParam("storyFile") MultipartFile storyFile,
                              HttpServletRequest request) {
        User currentUser = getLoggedInUser(request);
        if (currentUser == null) return "redirect:/login";
        if (storyFile == null || storyFile.isEmpty()) return "redirect:/?story=empty";

        String originalFileName = storyFile.getOriginalFilename();
        if (originalFileName == null || !originalFileName.contains(".")) return "redirect:/?story=invalid";

        String lowerCaseName = originalFileName.toLowerCase();
        boolean allowedImage = lowerCaseName.endsWith(".jpg")
                || lowerCaseName.endsWith(".jpeg")
                || lowerCaseName.endsWith(".png")
                || lowerCaseName.endsWith(".gif")
                || lowerCaseName.endsWith(".webp");
        boolean allowedVideo = lowerCaseName.endsWith(".mp4")
                || lowerCaseName.endsWith(".webm")
                || lowerCaseName.endsWith(".ogg");

        String mimeType = storyFile.getContentType();
        if ((!allowedImage && !allowedVideo)
                || mimeType == null
                || !(mimeType.startsWith("image/") || mimeType.startsWith("video/"))) {
            return "redirect:/?story=invalid";
        }

        try {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, 24);

            Story story = new Story();
            story.setUser(currentUser);
            story.setMediaUrl(s3StorageService.upload(storyFile, "stories"));
            story.setMediaType(allowedVideo ? "video" : "image");
            story.setCaption(sanitize(caption));
            story.setCreatedAt(new Date());
            story.setExpiresAt(calendar.getTime());
            storyRepository.save(story);
            messagingTemplate.convertAndSend("/topic/social", Map.of(
                    "type", "story_created",
                    "story", toStoryPayload(story),
                    "actorId", currentUser.getId()
            ));

            return "redirect:/?story=created";
        } catch (Exception e) {
            return "redirect:/?story=error";
        }
    }

    @PostMapping("/stories/delete/{id}")
    @ResponseBody
    @Transactional
    public String deleteStory(@PathVariable Long id, HttpServletRequest request) {
        User currentUser = getLoggedInUser(request);
        Story story = storyRepository.findById(id).orElse(null);
        if (currentUser == null || story == null) return "error";
        if (!story.getUser().getId().equals(currentUser.getId()) && !"ADMIN".equals(currentUser.getRole())) {
            return "forbidden";
        }
        Long storyId = story.getId();
        storyRepository.delete(story);
        messagingTemplate.convertAndSend("/topic/social", Map.of(
                "type", "story_deleted",
                "storyId", storyId,
                "actorId", currentUser.getId()
        ));
        return "deleted";
    }

    private Map<String, Object> toStoryPayload(Story story) {
        User user = story.getUser();
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("id", story.getId());
        payload.put("mediaUrl", story.getMediaUrl());
        payload.put("mediaType", story.getMediaType());
        payload.put("caption", story.getCaption() != null ? story.getCaption() : "");
        payload.put("fullName", user.getFullName() != null ? user.getFullName() : user.getUsername());
        payload.put("userId", user.getId());
        payload.put("username", user.getUsername());
        payload.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "https://abs.twimg.com/sticky/default_profile_images/default_profile_400x400.png");
        return payload;
    }

    private String sanitize(String value) {
        if (value == null) return "";
        return value.trim()
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
