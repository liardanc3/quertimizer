package com.quertimizer.community.presentation.dto.request;

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

    @Min(0)
    @Max(1000)
    private int page = 1;

    @Size(max = 100)
    private String search;

    @Size(max = 100)
    private String tag;

    @Pattern(regexp = "all|discussion|question|notice")
    private String category = "all";

    @Pattern(regexp = "default|latest|views|likes|comments")
    private String sortKey = "default";

    public CommunityPostSearchInput toInput() {
        return new CommunityPostSearchInput(page, search, tag, category, sortKey);
    }
}
