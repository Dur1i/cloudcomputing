package com.websocial.repositories;

import com.websocial.models.Comment;
import com.websocial.models.Post;
import com.websocial.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    java.util.List<Comment> findByPostOrderByCreatedAtAsc(Post post);
    long countByPostId(Long postId);
    
    @Transactional
    @Modifying
    void deleteByUser(User user);

    @Transactional
    @Modifying
    void deleteByPost(Post post);
}
