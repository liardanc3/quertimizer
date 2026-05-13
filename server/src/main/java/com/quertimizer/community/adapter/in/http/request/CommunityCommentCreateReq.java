package com.quertimizer.community.adapter.in.http.request;

import com.quertimizer.community.application.input.CommunityCommentInput;
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

    @NotBlank(message = "댓글 내용을 입력해 주세요.")
    @Size(max = 5000, message = "댓글은 최대 5000자까지 입력할 수 있습니다.")
    private String content;

    public CommunityCommentInput toCommunityCommentInput() {
        return new CommunityCommentInput(parentCommentId, content);
    }
}
