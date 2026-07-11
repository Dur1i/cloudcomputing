package com.websocial.config;

import com.websocial.models.User;
import com.websocial.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// Import thêm thư viện băm mật khẩu
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeeder {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            // Kiểm tra xem username 'admin' đã tồn tại chưa
            if (userRepository.findByUsername("admin") == null) {
                User admin = new User();
                admin.setFullName("Quản trị viên");
                admin.setUsername("admin");
                
                // VÁ LỖI: Băm mật khẩu mặc định bằng thuật toán BCrypt thay vì lưu Cleartext
                PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                admin.setPassword(passwordEncoder.encode("admin")); 
                
                admin.setEmail("admin@websocial.com");
                admin.setRole("ADMIN");
                admin.setBio("");
                
                userRepository.save(admin);
            }
        };
    }
}