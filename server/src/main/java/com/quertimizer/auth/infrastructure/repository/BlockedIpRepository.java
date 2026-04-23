package com.quertimizer.auth.infrastructure.repository;

import com.quertimizer.auth.domain.entity.BlockedIp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedIpRepository extends JpaRepository<BlockedIp, String> {

    boolean existsByIpAddress(String ipAddress);

    void deleteByBlockedHandle(String blockedHandle);

    Page<BlockedIp> findAllByOrderByBlockedAtDescIpAddressAsc(Pageable pageable);

}
