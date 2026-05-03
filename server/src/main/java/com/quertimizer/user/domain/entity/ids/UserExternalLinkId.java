package com.quertimizer.user.domain.entity.ids;

import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserExternalLinkId implements Serializable {

    private String handle;
    private String type;
    private String link;

    public static UserExternalLinkId create(String handle, String type, String link) {
        // 외부 링크 식별자 생성
        return new UserExternalLinkId(handle, type, link);
    }

}
