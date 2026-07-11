package com.websocial.models;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tạo liên kết (Khóa ngoại) tới bảng User
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Nội dung bài viết (sẽ chứa lỗ hổng XSS ở đây)
    @Column(columnDefinition = "TEXT")
    private String content;

    private String mediaUrl;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", updatable = false)
    private Date createdAt = new Date();

    // Bạn có thể dùng constructor để tự động set thời gian lúc tạo bài
    public Post() {
        this.createdAt = new Date();
    }

    // Tương tự, hãy dùng Insert Code để tạo Getter and Setter cho Post nhé.

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    // Thêm các biến này vào dưới biến createdAt trong Post.java
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private java.util.List<Comment> comments;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private java.util.List<PostLike> likes;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private java.util.List<Repost> reposts;

    // Nhớ Generate Getter/Setter cho 3 List mới này nhé!

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<PostLike> getLikes() {
        return likes;
    }

    public void setLikes(List<PostLike> likes) {
        this.likes = likes;
    }

    public List<Repost> getReposts() {
        return reposts;
    }

    public void setReposts(List<Repost> reposts) {
        this.reposts = reposts;
    }
    // Đếm số Comment thực tế từ Database
    @org.hibernate.annotations.Formula("(SELECT COUNT(*) FROM comments c WHERE c.post_id = id)")
    private int commentCount;

    // Đếm số Like thực tế từ Database
    @org.hibernate.annotations.Formula("(SELECT COUNT(*) FROM post_likes l WHERE l.post_id = id)")
    private int likeCount;

    // Nhớ tạo thêm Getter/Setter cho 2 biến này nhé (Alt + Insert -> Getter and Setter)
    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
}
