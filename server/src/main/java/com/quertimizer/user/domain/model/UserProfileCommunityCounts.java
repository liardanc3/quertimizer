package com.quertimizer.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileCommunityCounts {

    private final long authoredPostCount;
    private final long likedPostCount;
    private final long commentCount;

    public static UserProfileCommunityCounts empty() {
        return new UserProfileCommunityCounts(0, 0, 0);
    }

}
