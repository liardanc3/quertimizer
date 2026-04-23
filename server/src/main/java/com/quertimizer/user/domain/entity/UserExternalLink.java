package com.quertimizer.user.domain.entity;

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
public class UserExternalLink {

    @EmbeddedId
    private UserExternalLinkId id;

    public static UserExternalLink create(String handle, String type, String link) {
        return new UserExternalLink(UserExternalLinkId.create(handle, type, link));
    }

    public String getHandle() {
        return id.getHandle();
    }

    public String getType() {
        return id.getType();
    }

    public String getLink() {
        return id.getLink();
    }

    private UserExternalLink(UserExternalLinkId id) {
        this.id = id;
    }

}
