package com.quertimizer.community.presentation.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommunityTagSuggestionReq {

    @Size(max = 100)
    private String query;
}
