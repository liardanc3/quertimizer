package com.quertimizer.repository;

import com.quertimizer.entity.User;
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
    private UserRepository userRepository;

    @Nested
    @DisplayName("existsByUserId")
    class ExistsByUserId {

        @Test
        @DisplayName("true")
        void returnTrue() {
            // given
            String userId = uniqueUserId();
            userRepository.save(User.create(userId, "hashed-password", uniqueEmail()));

            // when
            boolean result = userRepository.existsByUserId(userId);

            // then
            assertTrue(result);
        }

        @Test
        @DisplayName("false")
        void returnFalse() {
            // given
            String userId = uniqueUserId();

            // when
            boolean result = userRepository.existsByUserId(userId);

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
            userRepository.save(User.create(uniqueUserId(), "hashed-password", email));

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

    private String uniqueUserId() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }
}
