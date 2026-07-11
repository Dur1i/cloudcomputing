package com.websocial.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.websocial.models.User;
import com.websocial.repositories.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // Secret Key đồng bộ toàn hệ thống
    private final String SECRET_KEY = "TCSocial#Secure#Key#2026!Pentest^&^";

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(User user, Model model) {
        try {
            String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
            if (user.getEmail() == null || !user.getEmail().matches(emailRegex)) {
                model.addAttribute("error", "Email không hợp lệ!");
                return "register";
            }

            String fullNameRegex =
                    "^[a-zA-Z0-9\\sÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠàáâãèéêìíòóôõùúăđĩũơƯĂẠẢẤẦẨẪẬẮẰẲẴẶẸẺẼỀỀỂưăạảấầẩẫậắằẳẵặẹẻẽềềểỄỆỈỊỌỎỐỒỔỖỘỚỜỞỠỢỤỦỨỪễệỉịọỏốồổỗộớờởỡợụủứừỬỮỰỲỴÝỶỸửữựỳỵỷỹ'\\-]{2,50}$";
            if (user.getFullName() == null || !user.getFullName().matches(fullNameRegex)) {
               model.addAttribute("error", "Tên hiển thị không hợp lệ! Chỉ cho phép 2-50 ký tự, không chứa ký tự đặc biệt nguy hiểm.");
                return "register";
            }

            // Regex username cho phép chữ, số, dấu gạch dưới; chặn các từ khóa quản trị
            String usernameRegex = "^[a-zA-Z0-9_]{3,20}$";
            String lowerUser = user.getUsername() == null ? "" : user.getUsername().toLowerCase();
            if (user.getUsername() == null || !user.getUsername().matches(usernameRegex) ||
                lowerUser.contains("admin") || lowerUser.contains("system") || lowerUser.contains("root")) {
                model.addAttribute("error", "Username không hợp lệ! Chỉ dùng chữ cái, số, dấu '_' và không chứa từ khóa quản trị (admin, root...).");
                return "register";
            }
            // ====================================================================

            // 2. Chính sách mật khẩu mạnh (OTG-IDENT-002)
            String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$";
            if (user.getPassword() == null || !user.getPassword().matches(passwordRegex)) {
                model.addAttribute("error", "Mật khẩu cần ít nhất 8 ký tự, có chữ hoa, thường, số và ký tự đặc biệt!");
                return "register";
            }

            if (userRepository.findByUsername(user.getUsername()) != null) {
                model.addAttribute("error", "Username đã tồn tại!");
                return "register";
            }

            // 3. Băm mật khẩu BCrypt
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setRole("USER");
            userRepository.save(user);
            return "redirect:/login?success";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            return "register";
        }
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String username, 
                               @RequestParam String password, 
                               HttpServletResponse response, 
                               Model model) {
        try {
            User user = userRepository.findByUsername(username);

            // TẤM KHIÊN: Kiểm tra khóa tài khoản (OTG-AUTHN-003)
            if (user != null && user.getLockTime() != null) {
                if (new Date().before(user.getLockTime())) {
                    model.addAttribute("error", "Tài khoản đang bị khóa. Vui lòng thử lại sau 15 phút.");
                    return "login";
                } else {
                    // Reset nếu đã qua thời gian phạt
                    user.setLockTime(null);
                    user.setFailedAttempts(0);
                    userRepository.save(user);
                }
            }

            boolean isPasswordMatch = false;
            if (user != null) {
                PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                try {
                    // Thử khớp theo chuẩn BCrypt
                    isPasswordMatch = passwordEncoder.matches(password, user.getPassword());
                } catch (Exception e) {
                    // Nếu pass trong DB chưa băm (dữ liệu cũ), dùng equals để tránh văng lỗi 500
                    isPasswordMatch = user.getPassword().equals(password);
                }
            }

            // TẤM KHIÊN: Chống Account Enumeration & Brute-force
            if (user == null || !isPasswordMatch) {
                if (user != null) {
                    int attempts = (user.getFailedAttempts() == null ? 0 : user.getFailedAttempts()) + 1;
                    user.setFailedAttempts(attempts);
                    
                    int maxAttempts = 5;
                    int remaining = maxAttempts - attempts;

                    if (attempts >= maxAttempts) {
                        user.setLockTime(new Date(System.currentTimeMillis() + (15 * 60 * 1000)));
                        model.addAttribute("error", "Tài khoản đã bị khóa 15 phút do nhập sai quá 5 lần!");
                    } else {
                        model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không chính xác!");
                        // model.addAttribute("error", "Sai mật khẩu! Bạn còn " + remaining + " lần thử trước khi bị khóa tài khoản.");
                    }
                    userRepository.save(user);
                } else {
                    // Trả về thông báo chung chung để chống dò quét username
                    model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không chính xác!");
                }
                return "login";
            }

            // Đăng nhập thành công -> Dọn dẹp bộ đếm
            user.setFailedAttempts(0);
            user.setLockTime(null);
            userRepository.save(user);

            // Tạo Token
            Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
            String token = JWT.create()
                    .withIssuer("TC")
                    .withClaim("userId", user.getId())
                    .withClaim("username", user.getUsername())
                    .withClaim("role", user.getRole())
                    .withExpiresAt(new Date(System.currentTimeMillis() + 864000000))
                    .sign(algorithm);

            // TẤM KHIÊN: Cookie bảo mật (OTG-AUTHN-001 & OTG-SESS-002)
            Cookie cookie = new Cookie("auth_token", token);
            cookie.setHttpOnly(true);           
            cookie.setSecure(true); // Chỉ gửi qua HTTPS
            cookie.setPath("/");
            cookie.setMaxAge(864000);
            response.addCookie(cookie);

            return "redirect:/";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Lỗi hệ thống không xác định!");
            return "login";
        }
    }
}