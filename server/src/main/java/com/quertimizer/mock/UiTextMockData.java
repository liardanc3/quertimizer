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
    private static final String NOTIFICATION_KEY = "NOTIFICATION";
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
                ),
                UiText.create(
                        NOTIFICATION_KEY,
                        "\uCFFC\uD2F0\uB9C8\uC774\uC800\uC758 \uC0C8\uB85C\uC6B4 \uC18C\uC2DD\uC744 \uD655\uC778\uD558\uC138\uC694.",
                        "kr",
                        "\uD5E4\uB354 \uC804\uAD11\uD310 \uBB38\uAD6C"
                ),
                UiText.create(
                        NOTIFICATION_KEY,
                        "Check out the latest updates from Quertimizer.",
                        DEFAULT_LANGUAGE,
                        "Header marquee message"
                )
        ));
    }

}
