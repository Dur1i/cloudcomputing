package com.websocial.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.websocial.models.*;
import com.websocial.repositories.*;
import com.websocial.services.S3StorageService;
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
            // Expose accepted friends as users for the template.
            model.addAttribute("users", friends.values());
            
        } catch (Exception e) {
            e.printStackTrace(); 
            model.addAttribute("pendingRequests", new ArrayList<>());
            model.addAttribute("users", new ArrayList<>());
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
            
            // If there is no follow relation yet, create an accepted friendship.
            if (check1.isEmpty() && check2.isEmpty()) {
                Friendship f = new Friendship();
                f.setRequester(sender);
                f.setReceiver(receiver.get());
                f.setStatus("ACCEPTED");
                friendshipRepository.save(f);
                return "success";
            } else {
                // If a relation is still pending, accept it so chat can work.
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

    // Listen for realtime chat messages sent to /app/chat
    @org.springframework.messaging.handler.annotation.MessageMapping("/chat")
    public void sendMessageRealTime(ChatMessageRequest request) {
        User sender = userRepository.findById(request.getSenderId()).orElse(null);
        User receiver = userRepository.findById(request.getReceiverId()).orElse(null);

        if (sender != null && receiver != null && request.getContent() != null && !request.getContent().isEmpty()) {
            // Save to database.
            ChatMessage msg = new ChatMessage();
            msg.setSender(sender);
            msg.setReceiver(receiver);
            msg.setContent(request.getContent());
            chatMessageRepository.save(msg);

            // Build payload sent back to clients.
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("content", msg.getContent());
            payload.put("senderId", sender.getId());
            payload.put("receiverId", receiver.getId());

            // Send the message to both receiver and sender channels.
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
    
    // Chat file upload endpoint.
    @PostMapping("/api/chat/upload")
    @ResponseBody
    public String uploadChatFile(@RequestParam("file") org.springframework.web.multipart.MultipartFile file, HttpServletRequest request) {
        User currentUser = getLoggedInUser(request);
        if (currentUser == null || file.isEmpty()) return "error";
        
        try {
            return s3StorageService.upload(file, "chat");
        } catch (Exception e) { 
            return "error"; 
        }
    }
    
}
