package com.quertimizer.community.application.output;

import lombok.Data;

import java.nio.file.Path;

@Data
public class CommunityImageOutput {

    private final String imageId;
    private final String imageUrl;
    private final Path resourcePath;
    private final String contentType;
}
