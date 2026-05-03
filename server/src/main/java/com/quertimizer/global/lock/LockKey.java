package com.quertimizer.global.lock;

public enum LockKey {
    SIGNUP, // 회원가입 시 같은 ID로 가입하려는 요청이 동시에 들어올 때 대비
    EMAIL,  // 로그인 중복요청 대비 (네트워크 이슈 시)
    CREATE_PROBLEM // 문제 업데이트 요청 여러번 발생 시 문제 ID 중복 및 재채점 중복발생 대비
}
