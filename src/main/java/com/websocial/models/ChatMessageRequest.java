package com.websocial.models;

public class ChatMessageRequest {
    private Long senderId;
    private Long receiverId;
    private String content;

    // Bắt buộc phải có Getter / Setter để Spring Boot đọc được JSON
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}