package com.quertimizer.endpoint.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class CommunityCommentCreateReq {

    private Long parentCommentId;

    @NotBlank(message = "댓글 내용을 입력해.")
    @Size(max = 5000, message = "댓글은 최대 5000자까지 입력할 수 있다.")
    private String content;

}
