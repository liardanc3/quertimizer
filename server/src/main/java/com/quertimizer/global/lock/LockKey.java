package com.quertimizer.global.lock;

public enum LockKey {
    SIGNUP, // 회원가입 시 같은 ID로 가입하려는 요청이 동시에 들어올 때 대비
    EMAIL,  // 로그인 중복요청 대비 (네트워크 이슈 시)
}
