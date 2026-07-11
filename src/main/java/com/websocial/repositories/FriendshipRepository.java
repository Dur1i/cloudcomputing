package com.websocial.repositories;

import com.websocial.models.Friendship;
import com.websocial.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    
    List<Friendship> findByReceiverAndStatus(User receiver, String status);

    @Query("SELECT f FROM Friendship f WHERE (f.requester = :user OR f.receiver = :user) AND f.status = 'ACCEPTED'")
    List<Friendship> findAllFriends(@Param("user") User user);

    // SỬA DÒNG NÀY: Trả về List thay vì 1 đối tượng duy nhất
    List<Friendship> findByRequesterAndReceiver(User requester, User receiver);
    void deleteByRequesterOrReceiver(User requester, User receiver);
    
    // Đếm số người mà User này đang theo dõi (User là người gửi yêu cầu)
    long countByRequesterAndStatus(com.websocial.models.User requester, String status);

    // Đếm số người đang theo dõi User này (User là người nhận yêu cầu)
    long countByReceiverAndStatus(com.websocial.models.User receiver, String status);
    
}