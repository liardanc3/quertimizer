package com.quertimizer.mock;

import com.quertimizer.constant.DbmsType;
import com.quertimizer.constant.UserRole;
import com.quertimizer.entity.User;
import com.quertimizer.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("userMockData")
@RequiredArgsConstructor
public class UserMockData {

    private static final String RAW_PASSWORD = "abcd1234!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void seed() {
        userRepository.saveAll(createUsers());
    }

    private List<User> createUsers() {
        List<User> users = new ArrayList<>();

        users.add(createUser(
                "admin",
                "admin@example.com",
                UserRole.ADMIN,
                "문제와 커뮤니티를 관리하는 운영자 계정",
                DbmsType.POSTGRESQL
        ));
        users.add(createUser(
                "liardanc3",
                "liardanc3@example.com",
                UserRole.ADMIN,
                "실행 계획과 성능 비교 기록을 정리하는 계정",
                DbmsType.POSTGRESQL
        ));

        for (int index = 1; index <= 10; index++) {
            users.add(createUser(
                    "beginner" + formatTwoDigits(index),
                    "beginner" + formatTwoDigits(index) + "@example.com",
                    UserRole.USER,
                    "기초 SQL 문제를 주로 푸는 사용자",
                    DbmsType.POSTGRESQL
            ));
            users.add(createUser(
                    "intermediate" + formatTwoDigits(index),
                    "intermediate" + formatTwoDigits(index) + "@example.com",
                    UserRole.USER,
                    "조인과 집계를 자주 연습하는 사용자",
                    index % 2 == 0 ? DbmsType.ORACLE : DbmsType.POSTGRESQL
            ));
            users.add(createUser(
                    "advanced" + formatTwoDigits(index),
                    "advanced" + formatTwoDigits(index) + "@example.com",
                    UserRole.USER,
                    "실행 계획과 인덱스 실험을 자주 하는 사용자",
                    DbmsType.ORACLE
            ));
        }

        return users;
    }

    private User createUser(String userId, String email, UserRole role, String bio, DbmsType defaultDbms) {
        User user = User.create(userId, encodeForClientLogin(RAW_PASSWORD), email);

        user.changeRole(role);
        user.changeProfile(bio, defaultDbms, false, true, true, true);
        return user;
    }

    private String encodeForClientLogin(String rawPassword) {
        return passwordEncoder.encode(Sha512DigestUtils.shaHex(rawPassword));
    }

    private String formatTwoDigits(int value) {
        return "%02d".formatted(value);
    }
}
