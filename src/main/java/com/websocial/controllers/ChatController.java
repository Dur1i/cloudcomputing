package com.websocial.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.websocial.models.*;
import com.websocial.repositories.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

@Controller
public class ChatController {

    @Autowired private UserRepository userRepository;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
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

    @GetMapping("/messages")
    public String messagesPage(HttpServletRequest request, Model model) {
        User currentUser = getLoggedInUser(request);
        if (currentUser == null) return "redirect:/login";
        model.addAttribute("currentUser", currentUser);

        try {
            List<Friendship> pending = friendshipRepository.findByReceiverAndStatus(currentUser, "PENDING");
            model.addAttribute("pendingRequests", (pending != null) ? pending : new ArrayList<>());

            List<Friendship> friendships = friendshipRepository.findAllFriends(currentUser);
            LinkedHashMap<Long, User> friends = new LinkedHashMap<>();
            
            if (friendships != null) {
                for (Friendship f : friendships) {
                    if (f.getRequester() != null && f.getRequester().getId().equals(currentUser.getId())) {
                        if (f.getReceiver() != null) friends.putIfAbsent(f.getReceiver().getId(), f.getReceiver());
                    } else if (f.getRequester() != null) {
                        friends.putIfAbsent(f.getRequester().getId(), f.getRequester());
                    }
                }
            }
            // 🛠️ ĐÃ SỬA CHỖ NÀY: Đổi "friends" thành "users" để HTML đọc được
            model.addAttribute("users", friends.values());
            
        } catch (Exception e) {
            e.printStackTrace(); 
            model.addAttribute("pendingRequests", new ArrayList<>());
            model.addAttribute("users", new ArrayList<>()); // 🛠️ Sửa luôn ở phần catch
        }
        return "messages";
    }

   @PostMapping("/api/friends/add/{receiverId}")
    @ResponseBody
    public String addFriend(@PathVariable("receiverId") Long receiverId, HttpServletRequest request) {
        User sender = getLoggedInUser(request);
        Optional<User> receiver = userRepository.findById(receiverId);
        
        if (sender != null && receiver.isPresent() && !sender.getId().equals(receiverId)) {
            List<Friendship> check1 = friendshipRepository.findByRequesterAndReceiver(sender, receiver.get());
            List<Friendship> check2 = friendshipRepository.findByRequesterAndReceiver(receiver.get(), sender);
            
            // Nếu chưa từng Follow -> Tạo mới và ACCEPTED luôn
            if (check1.isEmpty() && check2.isEmpty()) {
                Friendship f = new Friendship();
                f.setRequester(sender);
                f.setReceiver(receiver.get());
                f.setStatus("ACCEPTED"); // Bỏ qua PENDING, làm bạn luôn để test Chat!
                friendshipRepository.save(f);
                return "success";
            } else {
                // Nếu đang bị kẹt ở PENDING (do lỗi trước đó) -> Cập nhật thành ACCEPTED luôn
                if (!check1.isEmpty()) {
                    Friendship f = check1.get(0);
                    f.setStatus("ACCEPTED");
                    friendshipRepository.save(f);
                }
                if (!check2.isEmpty()) {
                    Friendship f = check2.get(0);
                    f.setStatus("ACCEPTED");
                    friendshipRepository.save(f);
                }
                return "success";
            }
        }
        return "error";
    }

    @PostMapping("/api/friends/accept/{friendshipId}")
    @Transactional 
    public String acceptFriend(@PathVariable("friendshipId") Long friendshipId) {
        try {
            Optional<Friendship> f = friendshipRepository.findById(friendshipId);
            if (f.isPresent()) {
                f.get().setStatus("ACCEPTED");
                friendshipRepository.save(f.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/messages";
    }

    @GetMapping("/api/chat/history/{user1Id}/{user2Id}")
    @ResponseBody
    public List<java.util.Map<String, Object>> getChatHistory(@PathVariable("user1Id") Long user1Id, @PathVariable("user2Id") Long user2Id) {
        User u1 = userRepository.findById(user1Id).orElse(null);
        User u2 = userRepository.findById(user2Id).orElse(null);
        List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        
        if (u1 != null && u2 != null) {
            List<ChatMessage> messages = chatMessageRepository.findBySenderAndReceiverOrReceiverAndSenderOrderBySentAtAsc(u1, u2, u1, u2);
            for (ChatMessage msg : messages) {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("content", msg.getContent());
                map.put("senderId", msg.getSender().getId());
                result.add(map);
            }
        }
        return result; 
    }

    @PostMapping("/api/chat/send")
    @ResponseBody
    public String sendMessage(@RequestParam("receiverId") Long receiverId, @RequestParam("content") String content, HttpServletRequest request) {
        User sender = getLoggedInUser(request);
        User receiver = userRepository.findById(receiverId).orElse(null);
        if (sender != null && receiver != null && !content.isEmpty()) {
            ChatMessage msg = new ChatMessage();
            msg.setSender(sender);
            msg.setReceiver(receiver);
            msg.setContent(content);
            chatMessageRepository.save(msg);
            return "success";
        }
        return "error";
    }
    
    @PostMapping("/api/friends/remove/{friendId}")
    @ResponseBody
    @Transactional
    public String removeFriend(@PathVariable("friendId") Long friendId, HttpServletRequest request) {
        User currentUser = getLoggedInUser(request);
        User friend = userRepository.findById(friendId).orElse(null);
        if (currentUser != null && friend != null) {
            List<Friendship> f1 = friendshipRepository.findByRequesterAndReceiver(currentUser, friend);
            List<Friendship> f2 = friendshipRepository.findByRequesterAndReceiver(friend, currentUser);
            friendshipRepository.deleteAll(f1);
            friendshipRepository.deleteAll(f2);
            return "success";
        }
        return "error";
    }

    // L?ng nghe c�c tin nh?n g?i d?n d�ch: /app/chat
    @org.springframework.messaging.handler.annotation.MessageMapping("/chat")
    public void sendMessageRealTime(ChatMessageRequest request) {
        User sender = userRepository.findById(request.getSenderId()).orElse(null);
        User receiver = userRepository.findById(request.getReceiverId()).orElse(null);

        if (sender != null && receiver != null && request.getContent() != null && !request.getContent().isEmpty()) {
            // Luu v�o Database
            ChatMessage msg = new ChatMessage();
            msg.setSender(sender);
            msg.setReceiver(receiver);
            msg.setContent(request.getContent());
            chatMessageRepository.save(msg);

            // ��ng g�i d? li?u b?n v? Client
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("content", msg.getContent());
            payload.put("senderId", sender.getId());
            payload.put("receiverId", receiver.getId());

            // B?n tin nh?n v�o k�nh c?a ngu?i nh?n V� ngu?i g?i
            messagingTemplate.convertAndSend("/topic/messages/" + receiver.getId(), payload);
            messagingTemplate.convertAndSend("/topic/messages/" + sender.getId(), payload);
        }
    }

    @org.springframework.messaging.handler.annotation.MessageMapping("/chat/typing")
    public void sendTypingStatus(java.util.Map<String, Object> request) {
        Long senderId = Long.valueOf(request.get("senderId").toString());
        Long receiverId = Long.valueOf(request.get("receiverId").toString());
        Boolean isTyping = (Boolean) request.get("isTyping");

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("type", "typing");
        payload.put("senderId", senderId);
        payload.put("receiverId", receiverId);
        payload.put("isTyping", isTyping);

        messagingTemplate.convertAndSend("/topic/messages/" + receiverId, payload);
    }
    
    // API Upload File ri�ng cho Chat (L? H?NG PENTEST: Kh�ng l?c d?nh d?ng file)
    @PostMapping("/api/chat/upload")
    @ResponseBody
    public String uploadChatFile(@RequestParam("file") org.springframework.web.multipart.MultipartFile file, HttpServletRequest request) {
        User currentUser = getLoggedInUser(request);
        if (currentUser == null || file.isEmpty()) return "error";
        
        try {
            String uploadsDir = System.getProperty("user.dir") + "/uploads/";
            java.io.File dir = new java.io.File(uploadsDir);
            if (!dir.exists()) dir.mkdirs();

            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
            }
            String newFileName = java.util.UUID.randomUUID().toString() + fileExtension;
            
            java.io.File dest = new java.io.File(uploadsDir + newFileName);
            file.transferTo(dest);
            
            return "/uploads/" + newFileName;
        } catch (Exception e) { 
            return "error"; 
        }
    }
    
}
