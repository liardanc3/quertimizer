package com.quertimizer.endpoint.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class CommunityPostSaveReq {

    @NotBlank(message = "게시글 제목을 입력해.")
    @Size(max = 200, message = "게시글 제목은 최대 200자까지 입력할 수 있다.")
    private String title;

    @Size(max = 100000, message = "게시글 본문은 최대 100000자까지 입력할 수 있다.")
    private String contentHtml;

    @Builder.Default
    @Size(max = 10, message = "태그는 최대 10개까지 추가할 수 있다.")
    private List<@NotBlank(message = "태그는 비워둘 수 없다.") @Size(max = 100, message = "태그는 최대 100자까지 입력할 수 있다.") String> tags =
            new ArrayList<>();

}
