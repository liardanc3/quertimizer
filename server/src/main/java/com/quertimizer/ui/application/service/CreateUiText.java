package com.quertimizer.ui.application.service;

import com.quertimizer.ui.application.port.in.CreateUiTextUseCase;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.ui.application.input.UiTextInput;
import com.quertimizer.ui.application.output.UiTextOutput;
import com.quertimizer.ui.application.port.out.UiTextRepositoryPort;
import com.quertimizer.ui.application.service.UiTextService;
import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.entity.ids.UiTextId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.quertimizer.ui.domain.model.UiTextFailReason.DESCRIPTION_REQUIRED;
import static com.quertimizer.ui.domain.model.UiTextFailReason.DUPLICATED_UI_TEXT;
import static com.quertimizer.ui.domain.model.UiTextFailReason.VALUE_REQUIRED;

@Component
@RequiredArgsConstructor
public class CreateUiText implements CreateUiTextUseCase {

    private final UiTextRepositoryPort uiTextRepository;
    private final UiTextService uiTextService;

    /**
     * UI 텍스트를 생성한다.
     *
     * @param input 생성할 UI 텍스트 입력
     */
    @Transactional
    @Override
    public UiTextOutput execute(UiTextInput input) {
        UiTextId uiTextId = uiTextService.createRequiredUiTextId(input.getKey(), input.getLanguage());

        if (uiTextRepository.existsById(uiTextId)) {
            throw new BusinessException(DUPLICATED_UI_TEXT.getMessage(), HttpStatus.CONFLICT);
        }

        return uiTextService.toOutput(uiTextRepository.save(UiText.create(
                uiTextId.getKey(), uiTextService.requireText(input.getValue(), VALUE_REQUIRED.getMessage()),
                uiTextId.getLanguage(), uiTextService.requireText(input.getDescription(), DESCRIPTION_REQUIRED.getMessage())
        )));
    }
}
