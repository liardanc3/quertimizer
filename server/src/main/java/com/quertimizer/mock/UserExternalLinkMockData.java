package com.quertimizer.mock;

import com.quertimizer.entity.UserExternalLink;
import com.quertimizer.repository.UserExternalLinkRepository;
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
        userExternalLinkRepository.saveAll(List.of(
                UserExternalLink.create("liardanc3", "blog", "https://blog.com/liardanc3"),
                UserExternalLink.create("liardanc3", "github", "https://github.com/liardanc3"),
                UserExternalLink.create("liardanc3", "email", "mailto:liardanc3@example.com"),
                UserExternalLink.create("admin", "blog", "https://blog.com/admin"),
                UserExternalLink.create("admin", "github", "https://github.com/admin")
        ));
    }
}
