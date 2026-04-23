package com.quertimizer.user.infrastructure.repository;

import com.quertimizer.user.domain.entity.UserExternalLink;
import com.quertimizer.user.domain.entity.UserExternalLinkId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserExternalLinkRepository extends JpaRepository<UserExternalLink, UserExternalLinkId> {

    List<UserExternalLink> findAllByIdHandleOrderByIdTypeAscIdLinkAsc(String handle);

    void deleteAllByIdHandle(String handle);

}
