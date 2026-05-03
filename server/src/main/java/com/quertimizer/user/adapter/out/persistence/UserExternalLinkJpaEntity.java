package com.quertimizer.user.adapter.out.persistence;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_external_link")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserExternalLinkJpaEntity {

    @EmbeddedId
    private UserExternalLinkJpaId id;

    public static UserExternalLinkJpaEntity create(String handle, String type, String link) {
        // 사용자 외부 링크 JPA 엔티티 생성
        return new UserExternalLinkJpaEntity(UserExternalLinkJpaId.create(handle, type, link));
    }

    private UserExternalLinkJpaEntity(UserExternalLinkJpaId id) {
        this.id = id;
    }
}
