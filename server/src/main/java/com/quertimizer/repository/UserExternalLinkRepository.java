package com.quertimizer.repository;

import com.quertimizer.entity.UserExternalLink;
import com.quertimizer.entity.UserExternalLinkId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserExternalLinkRepository extends JpaRepository<UserExternalLink, UserExternalLinkId> {

    List<UserExternalLink> findAllByIdUserIdOrderByIdTypeAscIdLinkAsc(String userId);

    void deleteAllByIdUserId(String userId);

}
