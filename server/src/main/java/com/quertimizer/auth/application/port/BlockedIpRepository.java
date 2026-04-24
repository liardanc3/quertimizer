package com.quertimizer.auth.application.port;

import com.quertimizer.auth.domain.entity.BlockedIp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface BlockedIpRepository {

    boolean existsByIpAddress(String ipAddress);

    void deleteByBlockedHandle(String blockedHandle);

    Page<BlockedIp> findAllByOrderByBlockedAtDescIpAddressAsc(Pageable pageable);

    Optional<BlockedIp> findById(String ipAddress);

    <S extends BlockedIp> S save(S blockedIp);

    void deleteById(String ipAddress);
}
