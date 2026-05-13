package com.quertimizer.community.adapter.in.http.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommunityTagSuggestionReq {

    @Size(max = 100, message = "태그 검색어는 최대 100자까지 입력할 수 있습니다.")
    private String query;
}
