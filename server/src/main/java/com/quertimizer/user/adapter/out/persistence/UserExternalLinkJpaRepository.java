package com.quertimizer.user.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserExternalLinkJpaRepository extends JpaRepository<UserExternalLinkJpaEntity, UserExternalLinkJpaId> {
    List<UserExternalLinkJpaEntity> findAllByIdHandleOrderByIdTypeAscIdLinkAsc(String handle);
    void deleteAllByIdHandle(String handle);
}
