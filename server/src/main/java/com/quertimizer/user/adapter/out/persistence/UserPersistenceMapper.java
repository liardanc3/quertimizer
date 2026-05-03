package com.quertimizer.user.adapter.out.persistence;

import com.quertimizer.user.domain.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceMapper {

    public User toDomain(UserJpaEntity entity) {
        // 사용자 JPA 엔티티를 도메인 엔티티로 변환
        return User.restore(
                entity.getEmail(), entity.getHandle(), entity.getPassword(),
                entity.getBio(), entity.getProfileImageUrl(), entity.getBackgroundImageUrl(),
                entity.getRole(), entity.getDefaultDbms(), entity.getSqlPublic(),
                entity.getExecutionPercentilePublic(), entity.getSolvedRecordsPublic(),
                entity.getSolvedProblemCountPublic(), entity.getCommunityActivityPublic(),
                entity.getSolvedProblemCount(), entity.getSolvedExecutionTimeSumMs(),
                entity.getSignupAt(), entity.getLastAccessIp(), entity.getLastAccessAt(),
                entity.getBlockedUser(), entity.getBlockedAt()
        );
    }

    public UserJpaEntity toEntity(User user) {
        // 사용자 도메인 엔티티를 JPA 엔티티로 변환
        return UserJpaEntity.create(
                user.getEmail(), user.getHandle(), user.getPassword(),
                user.getBio(), user.getProfileImageUrl(), user.getBackgroundImageUrl(),
                user.getRole(), user.getDefaultDbms(), user.getSqlPublic(),
                user.getExecutionPercentilePublic(), user.getSolvedRecordsPublic(),
                user.getSolvedProblemCountPublic(), user.getCommunityActivityPublic(),
                user.getSolvedProblemCount(), user.getSolvedExecutionTimeSumMs(),
                user.getSignupAt(), user.getLastAccessIp(), user.getLastAccessAt(),
                user.getBlockedUser(), user.getBlockedAt()
        );
    }

    public void updateEntity(UserJpaEntity entity, User user) {
        // 사용자 도메인 상태를 기존 JPA 엔티티에 반영
        entity.update(
                user.getHandle(), user.getPassword(), user.getBio(),
                user.getProfileImageUrl(), user.getBackgroundImageUrl(),
                user.getRole(), user.getDefaultDbms(), user.getSqlPublic(),
                user.getExecutionPercentilePublic(), user.getSolvedRecordsPublic(),
                user.getSolvedProblemCountPublic(), user.getCommunityActivityPublic(),
                user.getSolvedProblemCount(), user.getSolvedExecutionTimeSumMs(),
                user.getLastAccessIp(), user.getLastAccessAt(),
                user.getBlockedUser(), user.getBlockedAt()
        );
    }
}
