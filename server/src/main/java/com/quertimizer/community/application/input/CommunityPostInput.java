package com.quertimizer.community.application.input;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CommunityPostInput {

    private final String title;
    private final String contentHtml;
    private final List<String> tags;
}
