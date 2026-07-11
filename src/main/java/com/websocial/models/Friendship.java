package com.websocial.models;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "friendships")
public class Friendship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester; // Người gửi lời mời

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver; // Người nhận lời mời

    // Trạng thái: PENDING (đang chờ), ACCEPTED (đã làm bạn)
    private String status = "PENDING"; 

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    // Nhớ Generate Getter và Setter

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}