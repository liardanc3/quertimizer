package com.quertimizer.repository;

import com.quertimizer.entity.BlockedIp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedIpRepository extends JpaRepository<BlockedIp, String> {

    boolean existsByIpAddress(String ipAddress);

    void deleteByBlockedUserId(String blockedUserId);

    Page<BlockedIp> findAllByOrderByBlockedAtDescIpAddressAsc(Pageable pageable);

}
