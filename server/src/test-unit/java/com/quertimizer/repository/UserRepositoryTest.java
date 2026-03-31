package com.quertimizer.repository;

import com.quertimizer.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.autoconfigure.exclude=",
        "spring.datasource.url=jdbc:h2:mem:user-repository-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
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
            userRepository.save(User.create("tester", "hashed-password", "tester@example.com"));

            // when
            boolean result = userRepository.existsByUserId("tester");

            // then
            assertTrue(result);
        }

        @Test
        @DisplayName("false")
        void returnFalse() {
            // given
            // 아무것도 저장되어있지 않음

            // when
            boolean result = userRepository.existsByUserId("tester");

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
            userRepository.save(User.create("tester", "hashed-password", "tester@example.com"));

            // when
            boolean result = userRepository.existsByEmail("tester@example.com");

            // then
            assertTrue(result);
        }

        @Test
        @DisplayName("false")
        void returnFalse() {
            // given
            // 아무것도 저장되어있지 않음

            // when
            boolean result = userRepository.existsByEmail("tester@example.com");

            // then
            assertFalse(result);
        }
    }
}
