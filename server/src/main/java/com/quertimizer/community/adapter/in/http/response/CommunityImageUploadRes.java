package com.quertimizer.community.adapter.in.http.response;

import com.quertimizer.community.application.output.CommunityImageOutput;
import lombok.Data;

@Data
public class CommunityImageUploadRes {

    private final String imageId;
    private final String imageUrl;

    public static CommunityImageUploadRes from(CommunityImageOutput output, String imageUrl) {
        return new CommunityImageUploadRes(output.getImageId(), imageUrl);
    }
}
