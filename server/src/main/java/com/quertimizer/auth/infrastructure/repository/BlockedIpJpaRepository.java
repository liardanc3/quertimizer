package com.quertimizer.auth.infrastructure.repository;

import com.quertimizer.auth.application.port.BlockedIpRepository;
import com.quertimizer.auth.domain.entity.BlockedIp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedIpJpaRepository extends JpaRepository<BlockedIp, String>, BlockedIpRepository {
}
