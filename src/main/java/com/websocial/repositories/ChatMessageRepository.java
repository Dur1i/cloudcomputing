package com.websocial.repositories;
import com.websocial.models.ChatMessage;
import com.websocial.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySenderAndReceiverOrReceiverAndSenderOrderBySentAtAsc(User s1, User r1, User r2, User s2);
    void deleteBySenderOrReceiver(User sender, User receiver);
}