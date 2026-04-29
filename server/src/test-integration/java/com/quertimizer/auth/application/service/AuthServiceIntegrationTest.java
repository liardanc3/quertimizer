package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.VerificationCodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AuthService")
class AuthServiceIntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private VerificationCodeRepository verificationCodeRepository;

    @Nested
    @DisplayName("Scheduled deleteExpiredRecoveryCode")
    class DeleteExpiredRecoveryCode {

        @Test
        @DisplayName("성공 (만료 코드 삭제)")
        void successWhenExpiredCodesExist() {
            // given
            verificationCodeRepository.saveCode("expired@example.com", "ABC123", LocalDateTime.now().minusMinutes(1));
            verificationCodeRepository.saveCode("alive@example.com", "ZZ9999", LocalDateTime.now().plusMinutes(5));

            // when
            authService.deleteExpiredRecoveryCode();

            // then
            assertThat(verificationCodeRepository.findCode("expired@example.com")).isEmpty();
            assertThat(verificationCodeRepository.findCode("alive@example.com")).contains("ZZ9999");
        }
    }
}
