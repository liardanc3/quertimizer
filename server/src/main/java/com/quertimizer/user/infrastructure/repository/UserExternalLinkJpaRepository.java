package com.quertimizer.user.infrastructure.repository;

import com.quertimizer.user.application.port.UserExternalLinkRepository;
import com.quertimizer.user.domain.entity.UserExternalLink;
import com.quertimizer.user.domain.entity.UserExternalLinkId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserExternalLinkJpaRepository extends JpaRepository<UserExternalLink, UserExternalLinkId>, UserExternalLinkRepository {
}
