package com.websocial.models;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true)
    private String email;

    private String password;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String avatarUrl;

    @Column(nullable = false)
    private String role = "USER"; 

    // Trong User.java
    @Column(name = "failed_attempts")
    private Integer failedAttempts = 0; // Đổi int -> Integer và gán sẵn = 0

    @Column(name = "lock_time")
    private java.util.Date lockTime;

   
    
    // --- GETTERS & SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
     public Integer getFailedAttempts() { return failedAttempts; }

    public void setFailedAttempts(Integer failedAttempts) { this.failedAttempts = failedAttempts; }

    public Date getLockTime() { return lockTime; }

    public void setLockTime(Date lockTime) { this.lockTime = lockTime; }
}