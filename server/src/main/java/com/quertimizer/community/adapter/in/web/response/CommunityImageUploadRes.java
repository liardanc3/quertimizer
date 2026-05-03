package com.quertimizer.community.adapter.in.web.response;

import com.quertimizer.community.application.output.CommunityImageOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommunityImageUploadRes {

    private final String imageId;
    private final String imageUrl;

    public static CommunityImageUploadRes from(CommunityImageOutput output) {
        return new CommunityImageUploadRes(output.getImageId(), output.getImageUrl());
    }
}
