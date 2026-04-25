package com.quertimizer.community.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.Resource;

@Getter
@AllArgsConstructor
public class CommunityImageOutput {

    private final String imageId;
    private final String imageUrl;
    private final Resource resource;
    private final String contentType;
}
