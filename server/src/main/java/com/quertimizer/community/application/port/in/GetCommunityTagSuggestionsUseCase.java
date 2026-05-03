package com.quertimizer.community.application.port.in;

import com.quertimizer.community.application.output.CommunityTagSuggestionOutput;

import java.util.List;

public interface GetCommunityTagSuggestionsUseCase {

    List<CommunityTagSuggestionOutput> execute(String query);
}
