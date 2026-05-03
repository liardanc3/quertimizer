package com.quertimizer.community.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

@Getter
@AllArgsConstructor
public class CommunityImageOutput {

    private final String imageId;
    private final String imageUrl;
    private final Path resourcePath;
    private final String contentType;
}
