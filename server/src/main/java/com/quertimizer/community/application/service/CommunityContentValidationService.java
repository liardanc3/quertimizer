package com.quertimizer.community.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.community.domain.policy.CommunityContentPolicy;
import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.DomainRuleViolationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunityContentValidationService {

    private final ObjectMapper objectMapper;
    private final CommunityContentPolicy communityContentPolicy;

    public void validate(String contentJson) {
        // 커뮤니티 본문 JSON 파싱 후 도메인 정책 검증
        try {
            communityContentPolicy.validate(contentJson, objectMapper.readValue(contentJson, Object.class));
        } catch (JsonProcessingException exception) {
            throw new DomainRuleViolationException("본문 형식이 올바르지 않습니다.", DomainRuleViolationType.INVALID_REQUEST);
        }
    }
}
