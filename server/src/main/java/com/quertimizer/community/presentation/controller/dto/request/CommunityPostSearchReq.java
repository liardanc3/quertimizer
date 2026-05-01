package com.quertimizer.community.presentation.controller.dto.request;

import com.quertimizer.community.application.input.CommunityPostSearchInput;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommunityPostSearchReq {

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    @Max(value = 1000, message = "페이지 번호는 최대 1000까지 요청할 수 있습니다.")
    private int page = 1;

    @Size(max = 100, message = "검색어는 최대 100자까지 입력할 수 있습니다.")
    private String search;

    @Size(max = 100, message = "태그는 최대 100자까지 입력할 수 있습니다.")
    private String tag;

    @Pattern(regexp = "all|discussion|question|notice", message = "지원하지 않는 게시글 카테고리입니다.")
    private String category = "all";

    @Pattern(regexp = "default|latest|views|likes|comments", message = "지원하지 않는 게시글 정렬 기준입니다.")
    private String sortKey = "default";

    public CommunityPostSearchInput toInput() {
        return new CommunityPostSearchInput(page, search, tag, category, sortKey);
    }
}
