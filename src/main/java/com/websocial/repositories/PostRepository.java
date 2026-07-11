package com.websocial.repositories;

import com.websocial.models.Post;
import com.websocial.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // THÊM DÒNG NÀY
import org.springframework.transaction.annotation.Transactional; // THÊM DÒNG NÀY
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    @Transactional @Modifying
    void deleteByUser(User user);
    
    List<Post> findByUser(User user);
    
    List<Post> findByUserOrderByCreatedAtDesc(com.websocial.models.User user);
}