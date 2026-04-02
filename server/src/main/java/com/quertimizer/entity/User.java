package com.quertimizer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(nullable = false, length = 128)
    private String password;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "signup_at", nullable = false)
    private LocalDateTime signupAt;

    public static User create(String userId, String doubleHashedPassword, String email) {
        return new User(userId, doubleHashedPassword, email, LocalDateTime.now());
    }

    public void changePassword(String password) {
        this.password = password;
    }

    private User(String userId, String password, String email, LocalDateTime signupAt) {
        this.userId = userId;
        this.password = password;
        this.email = email;
        this.signupAt = signupAt;
    }

}
