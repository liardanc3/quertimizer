package com.quertimizer.service;

import com.quertimizer.endpoint.api.dto.request.SignupReq;
import com.quertimizer.entity.User;
import com.quertimizer.exception.BusinessException;
import com.quertimizer.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.token.Sha512DigestUtils;

import static com.quertimizer.constant.SignupFailReason.DUPLICATED_EMAIL;
import static com.quertimizer.constant.SignupFailReason.DUPLICATED_USER_ID;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Nested
    @DisplayName("signup")
    class Signup {

        @Nested
        @DisplayName("정상")
        class Normal {

            @Test
            @DisplayName("메소드 정상종료")
            void saveUser() {
                // given
                SignupReq request = SignupReq.builder()
                        .userId("tester")
                        .password("a".repeat(128))
                        .email("tester@example.com")
                        .build();

                when(userRepository.existsByUserId(request.getUserId())).thenReturn(false);
                when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

                // when
                userService.signup(request);

                // then
                ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
                verify(userRepository).save(captor.capture());

                User savedUser = captor.getValue();
                assertAll(
                        () -> assertEquals(request.getUserId(), savedUser.getUserId()),
                        () -> assertEquals(request.getEmail(), savedUser.getEmail()),
                        () -> assertEquals(Sha512DigestUtils.shaHex(request.getPassword()), savedUser.getPassword())
                );
            }
        }

        @Nested
        @DisplayName("예외")
        class ExceptionCase {

            @Test
            @DisplayName("BusinessException 발생 : userId 중복")
            void throwDuplicatedUserId() {
                // given
                SignupReq request = SignupReq.builder()
                        .userId("tester")
                        .password("a".repeat(128))
                        .email("tester@example.com")
                        .build();

                when(userRepository.existsByUserId(request.getUserId())).thenReturn(true);

                // when
                BusinessException exception = assertThrows(BusinessException.class, () -> userService.signup(request));

                // then
                assertAll(
                        () -> assertEquals(DUPLICATED_USER_ID, exception.getReason()),
                        () -> assertEquals(HttpStatus.CONFLICT, exception.getStatusCode())
                );
                verify(userRepository, never()).save(any(User.class));
            }

            @Test
            @DisplayName("BusinessException 발생 : email 중복")
            void throwDuplicatedEmail() {
                // given
                SignupReq request = SignupReq.builder()
                        .userId("tester")
                        .password("a".repeat(128))
                        .email("tester@example.com")
                        .build();

                when(userRepository.existsByUserId(request.getUserId())).thenReturn(false);
                when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

                // when
                BusinessException exception = assertThrows(BusinessException.class, () -> userService.signup(request));

                // then
                assertAll(
                        () -> assertEquals(DUPLICATED_EMAIL, exception.getReason()),
                        () -> assertEquals(HttpStatus.CONFLICT, exception.getStatusCode())
                );
                verify(userRepository, never()).save(any(User.class));
            }
        }
    }
}
