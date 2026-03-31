package com.quertimizer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.token.Sha512DigestUtils;

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

    // 서버단에서 SHA512 한번 더 해서 저장
    public static User create(String userId, String doubleHashedPassword, String email) {
        return new User(userId, doubleHashedPassword, email);
    }

    private User(String userId, String password, String email) {
        this.userId = userId;
        this.password = password;
        this.email = email;
    }

}
