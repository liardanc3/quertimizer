package com.quertimizer.user.infrastructure.repository;

import com.quertimizer.user.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@DataJpaTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserJpaRepository userRepository;

    @Nested
    @DisplayName("existsByHandle")
    class ExistsByHandle {

        @Test
        @DisplayName("true")
        void returnTrue() {
            // given
            String handle = uniqueHandle();
            userRepository.save(User.create(handle, "hashed-password", uniqueEmail()));

            // when
            boolean result = userRepository.existsByHandle(handle);

            // then
            assertTrue(result);
        }

        @Test
        @DisplayName("false")
        void returnFalse() {
            // given
            String handle = uniqueHandle();

            // when
            boolean result = userRepository.existsByHandle(handle);

            // then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmail {

        @Test
        @DisplayName("true")
        void returnTrue() {
            // given
            String email = uniqueEmail();
            userRepository.save(User.create(uniqueHandle(), "hashed-password", email));

            // when
            boolean result = userRepository.existsByEmail(email);

            // then
            assertTrue(result);
        }

        @Test
        @DisplayName("false")
        void returnFalse() {
            // given
            String email = uniqueEmail();

            // when
            boolean result = userRepository.existsByEmail(email);

            // then
            assertFalse(result);
        }
    }

    private String uniqueHandle() {
        // 테스트 간 충돌을 피하기 위해 충분히 긴 랜덤 handle를 생성한다.
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String uniqueEmail() {
        // 이메일 중복 검사 테스트에서 매번 다른 주소를 사용한다.
        return UUID.randomUUID() + "@example.com";
    }
}
