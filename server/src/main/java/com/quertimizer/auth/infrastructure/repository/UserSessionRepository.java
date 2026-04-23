package com.quertimizer.auth.infrastructure.repository;

import com.quertimizer.auth.domain.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

// 서버 재기동 이후 세션 복구에 사용할 sessionId -> handle 매핑을 저장한다.
public interface UserSessionRepository extends JpaRepository<UserSession, String> {
}
