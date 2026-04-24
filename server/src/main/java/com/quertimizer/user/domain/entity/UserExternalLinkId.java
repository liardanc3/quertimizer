package com.quertimizer.user.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserExternalLinkId implements Serializable {

    @Column(name = "handle", nullable = false, length = 50)
    private String handle;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "link", nullable = false, length = 255)
    private String link;

    public static UserExternalLinkId create(String handle, String type, String link) {
        // 외부 링크 식별자 생성
        return new UserExternalLinkId(handle, type, link);
    }

}
