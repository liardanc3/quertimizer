package com.quertimizer.user.infrastructure.repository;

import com.quertimizer.user.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    // 로그인과 세션 복원에서 email principal 기준으로 사용자를 조회한다.
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    // Handle 설정, 프로필 조회, Handle 중복 검사에서 handle 기준 조회를 사용한다.
    Optional<User> findByHandle(String handle);

    List<User> findTop20ByHandleContainingIgnoreCaseOrderByHandleAsc(String handle);

    List<User> findAllByHandleIn(List<String> handles);

    // 회원가입, Handle 설정 시 중복 검사에 사용한다.
    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByHandle(String handle);

    List<User> findAllByOrderByHandleAsc();

    Page<User> findAllByBlockedUserTrueOrderByBlockedAtDescHandleAsc(Pageable pageable);
}
