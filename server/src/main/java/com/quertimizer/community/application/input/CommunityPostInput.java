package com.quertimizer.community.application.input;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CommunityPostInput {

    private final String title;
    private final String contentJson;
    private final String plainTextSummary;
    private final List<String> imageIds;
    private final List<String> tags;
    private final String category;
}
