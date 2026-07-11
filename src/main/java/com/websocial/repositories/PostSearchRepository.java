package com.websocial.repositories;

import com.websocial.models.Post;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class PostSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Post> searchPosts(String keyword) {
        // VÁ LỖI: Thay thế phần nối chuỗi bằng tham số được đặt tên là :searchKeyword
        String sql = "SELECT *, 0 AS commentCount, 0 AS likeCount FROM posts WHERE content LIKE :searchKeyword";
        
        Query query = entityManager.createNativeQuery(sql, Post.class);
        
        // TRUYỀN DỮ LIỆU AN TOÀN: Gắn biến keyword vào tham số, cộng thêm dấu % ở 2 đầu để tìm kiếm LIKE
        // Hệ thống quản trị CSDL sẽ tự động escape (mã hóa) các ký tự nhạy cảm như ' hay "
        query.setParameter("searchKeyword", "%" + keyword + "%");
        
        return query.getResultList();
    }
}