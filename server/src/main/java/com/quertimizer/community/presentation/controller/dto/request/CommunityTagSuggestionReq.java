package com.quertimizer.community.presentation.controller.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommunityTagSuggestionReq {

    @Size(max = 100, message = "태그 검색어는 최대 100자까지 입력할 수 있습니다.")
    private String query;
}
