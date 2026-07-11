package com.websocial.repositories;

import com.websocial.models.Post;
import com.websocial.models.PostLike;
import com.websocial.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // THÊM DÒNG NÀY
import org.springframework.transaction.annotation.Transactional; // THÊM DÒNG NÀY

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    java.util.List<PostLike> findByUserId(Long userId);
    long countByPostId(Long postId);
    
    @Transactional @Modifying
    void deleteByUserIdAndPostId(Long userId, Long postId);
    
    @Transactional @Modifying
    void deleteByUser(User user);
    
    @Transactional @Modifying
    void deleteByPost(Post post);
}
