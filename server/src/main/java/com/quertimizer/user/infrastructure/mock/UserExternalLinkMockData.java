package com.quertimizer.user.infrastructure.mock;

import com.quertimizer.user.domain.entity.UserExternalLink;
import com.quertimizer.user.application.port.UserExternalLinkRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("userExternalLinkMockData")
@DependsOn("userMockData")
@RequiredArgsConstructor
public class UserExternalLinkMockData {

    private final UserExternalLinkRepository userExternalLinkRepository;

    @PostConstruct
    public void seed() {
        // 기본 외부 링크 Mock 데이터 적재
        userExternalLinkRepository.saveAll(List.of(
                UserExternalLink.create("liardanc3", "blog", "https://blog.com/liardanc3"),
                UserExternalLink.create("liardanc3", "github", "https://github.com/liardanc3"),
                UserExternalLink.create("liardanc3", "email", "mailto:liardanc3@example.com"),
                UserExternalLink.create("admin", "blog", "https://blog.com/admin"),
                UserExternalLink.create("admin", "github", "https://github.com/admin")
        ));
    }
}
