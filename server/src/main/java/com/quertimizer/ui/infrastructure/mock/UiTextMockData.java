package com.quertimizer.ui.infrastructure.mock;

import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.model.UiTextKey;
import com.quertimizer.ui.domain.model.UiTextLanguage;
import com.quertimizer.ui.application.port.UiTextRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("uiTextMockData")
@RequiredArgsConstructor
public class UiTextMockData {

    private final UiTextRepository uiTextRepository;

    @PostConstruct
    public void seed() {
        // 기본 UI 텍스트 Mock 데이터 적재
        uiTextRepository.saveAll(List.of(
                UiText.create(
                        UiTextKey.TITLE.getValue(),
                        "쿼티마이저",
                        "kr",
                        "사이트 제목"
                ),
                UiText.create(
                        UiTextKey.TITLE.getValue(),
                        "Quertimizer",
                        UiTextLanguage.DEFAULT.getValue(),
                        "사이트 제목(영어)"
                ),
                UiText.create(
                        UiTextKey.NOTIFICATION.getValue(),
                        "쿼티마이저의 새로운 소식을 확인하세요.",
                        "kr",
                        "헤더 전광판 문구"
                ),
                UiText.create(
                        UiTextKey.NOTIFICATION.getValue(),
                        "Check out the latest updates from Quertimizer.",
                        UiTextLanguage.DEFAULT.getValue(),
                        "Header marquee message"
                )
        ));
    }

}
