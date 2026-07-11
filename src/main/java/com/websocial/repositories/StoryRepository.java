package com.websocial.repositories;

import com.websocial.models.Story;
import com.websocial.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface StoryRepository extends JpaRepository<Story, Long> {
    List<Story> findByExpiresAtAfterOrderByCreatedAtDesc(Date now);

    List<Story> findByUserAndExpiresAtAfterOrderByCreatedAtDesc(User user, Date now);

    @Transactional
    @Modifying
    void deleteByUser(User user);
}
