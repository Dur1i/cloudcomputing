package com.websocial.repositories;
import com.websocial.models.Post;
import com.websocial.models.Repost;
import com.websocial.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RepostRepository extends JpaRepository<Repost, Long> {
    void deleteByUser(User user);
    void deleteByPost(Post post);
}