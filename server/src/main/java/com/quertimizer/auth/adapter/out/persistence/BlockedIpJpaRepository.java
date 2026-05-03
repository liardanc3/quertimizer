package com.quertimizer.auth.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedIpJpaRepository extends JpaRepository<BlockedIpJpaEntity, String> {
    boolean existsByIpAddress(String ipAddress);
    void deleteByBlockedHandle(String blockedHandle);
    Page<BlockedIpJpaEntity> findAllByOrderByBlockedAtDescIpAddressAsc(Pageable pageable);
}
