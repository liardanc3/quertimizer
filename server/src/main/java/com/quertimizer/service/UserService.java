package com.quertimizer.service;

import com.quertimizer.endpoint.api.dto.request.SignupReq;
import com.quertimizer.lock.LockKey;
import com.quertimizer.entity.User;
import com.quertimizer.exception.BusinessException;
import com.quertimizer.lock.Lock;
import com.quertimizer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.quertimizer.constant.SignupFailReason.DUPLICATED_EMAIL;
import static com.quertimizer.constant.SignupFailReason.DUPLICATED_USER_ID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Lock(prefix = LockKey.SIGNUP, key = "#p0.userId", timeout = 500)
    @Transactional
    public void signup(SignupReq request) {

        // userId 중복 검증
        if (userRepository.existsByUserId(request.getUserId())) {
            throw new BusinessException(DUPLICATED_USER_ID, HttpStatus.CONFLICT);
        }

        // Email 중복 검증
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(DUPLICATED_EMAIL, HttpStatus.CONFLICT);
        }

        // 유저 생성 후 저장
        User user = User.create(request.getUserId(), Sha512DigestUtils.shaHex(request.getPassword()), request.getEmail());
        userRepository.save(user);
    }
}
