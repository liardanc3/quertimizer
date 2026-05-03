package com.quertimizer.community.application.service;

import com.quertimizer.community.application.port.in.GetCommunityTagSuggestionsUseCase;
import com.quertimizer.community.application.output.CommunityTagSuggestionOutput;
import com.quertimizer.community.application.port.out.CommunityPostTagRepositoryPort;
import com.quertimizer.community.domain.entity.CommunityPostTag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetCommunityTagSuggestions implements GetCommunityTagSuggestionsUseCase {

    private final CommunityPostTagRepositoryPort communityPostTagRepository;

    /**
     * 게시글 태그 자동완성 후보를 조회한다.
     *
     * <ol>
     *   <li>검색어 존재 여부 검사
     *   <li>태그 사용 횟수 집계
     *   <li>사용 횟수와 태그명 기준 정렬
     * </ol>
     *
     * @param query 태그 검색어
     */
    @Transactional(readOnly = true)
    @Override
    public List<CommunityTagSuggestionOutput> execute(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        Map<String, Long> usageCountByTag = new LinkedHashMap<>();
        for (CommunityPostTag postTag : communityPostTagRepository.findAllByTagContainingIgnoreCaseOrderByTagAsc(query.trim())) {
            usageCountByTag.merge(postTag.getTag(), 1L, Long::sum);
        }

        return usageCountByTag.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .limit(10)
                .map(entry -> new CommunityTagSuggestionOutput(entry.getKey(), entry.getValue()))
                .toList();
    }
}
