package com.websocial.repositories;

import com.websocial.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data JPA tự động tạo câu lệnh SQL an toàn cho các hàm tìm kiếm này
    User findByUsername(String username);
    User findByEmail(String email);
    // Tìm kiếm theo tên hiển thị hoặc username
    java.util.List<User> findByUsernameContainingOrFullNameContaining(String username, String fullName);
    
    // Câu lệnh SQL để đếm số post theo userId
    
    
    // Dùng nativeQuery = true để chạy thẳng lệnh SQL xuống Database
    @org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(*) FROM posts WHERE user_id = :userId", nativeQuery = true)
    long countPostsByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
    
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% AND u.role != 'ADMIN'")
    java.util.List<User> searchUsersExcludingAdmin(@org.springframework.data.repository.query.Param("keyword") String keyword);
    
}