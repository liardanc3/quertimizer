package com.quertimizer.community.application.input;

import lombok.Data;

import java.util.List;

@Data
public class CommunityPostInput {

    private final String title;
    private final String contentJson;
    private final String plainTextSummary;
    private final List<String> imageIds;
    private final List<String> tags;
    private final String category;
}
