package com.quertimizer.endpoint.api.controller;

import com.quertimizer.endpoint.api.dto.request.SignupReq;
import com.quertimizer.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupReq request) {
        log.info("회원가입 요청 id : {}, email : {}", request.getUserId(), request.getEmail());
        userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
