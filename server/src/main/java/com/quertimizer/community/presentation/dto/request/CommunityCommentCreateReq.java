package com.quertimizer.community.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.quertimizer.community.domain.model.CommunityValidationMessage.COMMENT_LENGTH_EXCEEDED;
import static com.quertimizer.community.domain.model.CommunityValidationMessage.COMMENT_REQUIRED;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class CommunityCommentCreateReq {

    private Long parentCommentId;

    @NotBlank(message = COMMENT_REQUIRED)
    @Size(max = 5000, message = COMMENT_LENGTH_EXCEEDED)
    private String content;

}
