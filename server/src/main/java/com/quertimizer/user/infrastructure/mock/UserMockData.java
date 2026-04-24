package com.quertimizer.user.infrastructure.mock;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.global.constant.UserRole;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.user.application.port.UserRepository;
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
        // 로그인, 권한 설정, 커뮤니티/프로필 화면 확인에 사용할 기본 계정을 채운다.
        userRepository.saveAll(createUsers());
    }

    private List<User> createUsers() {
        // 사용자 목록 생성
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
        users.add(createUser(
                "problemgen01",
                "problemgen01@example.com",
                UserRole.PROBLEM_GENERATOR,
                "문제 출제와 검수를 담당하는 계정",
                DbmsType.POSTGRESQL
        ));
        users.add(createUser(
                "problemgen02",
                "problemgen02@example.com",
                UserRole.PROBLEM_GENERATOR,
                "문제셋 구성과 예시 데이터를 관리하는 계정",
                DbmsType.ORACLE
        ));
        users.add(createUser(
                "problemgen03",
                "problemgen03@example.com",
                UserRole.PROBLEM_GENERATOR,
                "문제 문구와 정답 SQL을 검토하는 계정",
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

    private User createUser(String handle, String email, UserRole role, String bio, DbmsType defaultDbms) {
        // 실제 로그인 흐름과 같은 이중 해시 비밀번호 형식을 사용해야 mock 계정으로도 로그인할 수 있다.
        User user = User.create(handle, encodeForClientLogin(RAW_PASSWORD), email);

        user.changeRole(role);
        user.changeProfile(bio, defaultDbms, false, true, true, true);
        return user;
    }

    private String encodeForClientLogin(String rawPassword) {
        // 클라이언트 1차 해시 + 서버 2차 해시 구조를 mock 데이터에도 동일하게 적용한다.
        return passwordEncoder.encode(Sha512DigestUtils.shaHex(rawPassword));
    }

    private String formatTwoDigits(int value) {
        // Two Digits 포맷
        return "%02d".formatted(value);
    }
}
