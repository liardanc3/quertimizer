package com.quertimizer.user.domain.entity;

import com.quertimizer.user.domain.entity.ids.UserExternalLinkId;
import lombok.Getter;

@Getter
public class UserExternalLink {

    private UserExternalLinkId id;

    public static UserExternalLink create(String handle, String type, String link) {
        // 외부 링크 생성
        return new UserExternalLink(UserExternalLinkId.create(handle, type, link));
    }

    public static UserExternalLink restore(String handle, String type, String link) {
        // 저장된 외부 링크 상태 복원
        return new UserExternalLink(UserExternalLinkId.create(handle, type, link));
    }

    public String getHandle() {
        // Handle 조회
        return id.getHandle();
    }

    public String getType() {
        // 유형 조회
        return id.getType();
    }

    public String getLink() {
        // 링크 조회
        return id.getLink();
    }

    private UserExternalLink(UserExternalLinkId id) {
        this.id = id;
    }

}
