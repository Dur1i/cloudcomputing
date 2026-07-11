package com.websocial.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.websocial.models.User;
import com.websocial.repositories.PostSearchRepository;
import com.websocial.repositories.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ExploreController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostSearchRepository postSearchRepository;

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

    @GetMapping("/explore")
    public String explorePage(@RequestParam(value = "q", required = false) String keyword, 
                              HttpServletRequest request, 
                              Model model) {
        User currentUser = getLoggedInUser(request);
        if (currentUser == null) return "redirect:/login";

        model.addAttribute("currentUser", currentUser);

        if (keyword != null && !keyword.isEmpty()) {
            
            // 1. CHUẨN HÓA VÀ LỌC DỮ LIỆU ĐẦU VÀO (Input Validation)
            String safeKeyword = keyword.trim();
            
            // Cắt độ dài tối đa để chống DoS
            if (safeKeyword.length() > 50) {
                safeKeyword = safeKeyword.substring(0, 50);
            }
            
            // Dùng Regex chặn các ký tự SQL Injection cơ bản (chỉ cho phép chữ, số, khoảng trắng)
            if (!safeKeyword.matches("[\\p{L}0-9\\s_\\-\\.]+")) {
                model.addAttribute("posts", null);
                model.addAttribute("users", null);
                model.addAttribute("keyword", "Ký tự không hợp lệ!");
                return "explore";
            }

            // Chốt thành biến final để đưa vào hàm Lambda
            final String finalKeyword = safeKeyword;

            // 2. GỌI XUỐNG REPOSITORY (Chắc chắn rằng Repo đã dùng Parameterized Query)
            model.addAttribute("posts", postSearchRepository.searchPosts(finalKeyword));
            
            // Lọc User an toàn
            List<User> searchResults = userRepository.findAll().stream()
                    .filter(u -> u.getUsername().toLowerCase().contains(finalKeyword.toLowerCase()) 
                              && !"ADMIN".equals(u.getRole()))
                    .collect(Collectors.toList());
                    
            model.addAttribute("users", searchResults);            
            model.addAttribute("keyword", finalKeyword);
            
        } else {
            model.addAttribute("posts", null); 
            model.addAttribute("users", null); 
        }

        return "explore";
    }
}