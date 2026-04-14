package com.quertimizer.mock;

import com.quertimizer.entity.UiText;
import com.quertimizer.repository.UiTextRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("uiTextMockData")
@RequiredArgsConstructor
public class UiTextMockData {

    private static final String TITLE_KEY = "TITLE";
    private static final String DEFAULT_LANGUAGE = "default";

    private final UiTextRepository uiTextRepository;

    @PostConstruct
    public void seed() {
        uiTextRepository.saveAll(List.of(
                UiText.create(
                        TITLE_KEY,
                        "\uCFFC\uD2F0\uB9C8\uC774\uC800",
                        "kr",
                        "\uC0AC\uC774\uD2B8 \uC81C\uBAA9"
                ),
                UiText.create(
                        TITLE_KEY,
                        "Quertimizer",
                        DEFAULT_LANGUAGE,
                        "\uC0AC\uC774\uD2B8 \uC81C\uBAA9(\uC601\uC5B4)"
                )
        ));
    }

}
