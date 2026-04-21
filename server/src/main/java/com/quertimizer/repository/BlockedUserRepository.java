package com.quertimizer.repository;

import com.quertimizer.entity.BlockedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, String> {

    boolean existsByUserId(String userId);

    Page<BlockedUser> findAllByOrderByBlockedAtDescUserIdAsc(Pageable pageable);

}
