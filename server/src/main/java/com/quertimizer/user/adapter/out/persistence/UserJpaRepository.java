package com.quertimizer.user.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {
    Optional<UserJpaEntity> findByEmail(String email);
    Optional<UserJpaEntity> findByEmailIgnoreCase(String email);
    Optional<UserJpaEntity> findByHandle(String handle);
    List<UserJpaEntity> findTop20ByHandleContainingIgnoreCaseOrderByHandleAsc(String handle);
    List<UserJpaEntity> findAllByHandleIn(List<String> handles);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByHandle(String handle);
    List<UserJpaEntity> findAllByOrderByHandleAsc();
    Page<UserJpaEntity> findAllByBlockedUserTrueOrderByBlockedAtDescHandleAsc(Pageable pageable);
}
