package com.quertimizer.community.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static com.quertimizer.community.domain.model.CommunityValidationMessage.POST_CONTENT_LENGTH_EXCEEDED;
import static com.quertimizer.community.domain.model.CommunityValidationMessage.POST_TITLE_LENGTH_EXCEEDED;
import static com.quertimizer.community.domain.model.CommunityValidationMessage.POST_TITLE_REQUIRED;
import static com.quertimizer.community.domain.model.CommunityValidationMessage.TAG_LENGTH_EXCEEDED;
import static com.quertimizer.community.domain.model.CommunityValidationMessage.TAG_LIMIT_EXCEEDED;
import static com.quertimizer.community.domain.model.CommunityValidationMessage.TAG_REQUIRED;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class CommunityPostSaveReq {

    @NotBlank(message = POST_TITLE_REQUIRED)
    @Size(max = 200, message = POST_TITLE_LENGTH_EXCEEDED)
    private String title;

    @Size(max = 100000, message = POST_CONTENT_LENGTH_EXCEEDED)
    private String contentHtml;

    @Builder.Default
    @Size(max = 10, message = TAG_LIMIT_EXCEEDED)
    private List<@NotBlank(message = TAG_REQUIRED) @Size(max = 100, message = TAG_LENGTH_EXCEEDED) String> tags =
            new ArrayList<>();

}
