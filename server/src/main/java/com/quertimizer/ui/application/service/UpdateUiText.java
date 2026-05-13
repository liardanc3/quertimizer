package com.quertimizer.ui.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.ui.application.port.in.UpdateUiTextUseCase;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.ui.application.input.UpdateUiTextInput;
import com.quertimizer.ui.application.output.UiTextOutput;
import com.quertimizer.ui.application.port.out.UiTextRepositoryPort;
import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.entity.ids.UiTextId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.quertimizer.ui.domain.model.UiTextFailReason.DESCRIPTION_REQUIRED;
import static com.quertimizer.ui.domain.model.UiTextFailReason.DUPLICATED_UI_TEXT;
import static com.quertimizer.ui.domain.model.UiTextFailReason.UI_TEXT_NOT_FOUND;
import static com.quertimizer.ui.domain.model.UiTextFailReason.VALUE_REQUIRED;

@Component
@RequiredArgsConstructor
public class UpdateUiText implements UpdateUiTextUseCase {

    private final UiTextRepositoryPort uiTextRepository;
    private final UiTextService uiTextService;

    /**
     * UI 텍스트를 수정한다.
     *
     * @param input 수정할 UI 텍스트 key, 언어, 내용 입력
     */
    @Transactional
    @Override
    @Log("UI 텍스트 수정")
    public UiTextOutput execute(UpdateUiTextInput input) {
        UiTextId originalUiTextId = uiTextService.createRequiredUiTextId(input.getKey(), input.getLanguage());
        UiText existingUiText = uiTextRepository.findById(originalUiTextId)
                .orElseThrow(() -> new BusinessException(UI_TEXT_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        UiTextId nextUiTextId = uiTextService.createRequiredUiTextId(input.getText().getKey(), input.getText().getLanguage());
        String value = uiTextService.requireText(input.getText().getValue(), VALUE_REQUIRED.getMessage());
        String description = uiTextService.requireText(input.getText().getDescription(), DESCRIPTION_REQUIRED.getMessage());

        if (originalUiTextId.equals(nextUiTextId)) {
            existingUiText.changeContent(value, description);
            return uiTextService.toOutput(uiTextRepository.save(existingUiText));
        }

        if (uiTextRepository.existsById(nextUiTextId)) {
            throw new BusinessException(DUPLICATED_UI_TEXT.getMessage(), HttpStatus.CONFLICT);
        }

        uiTextRepository.delete(existingUiText);
        return uiTextService.toOutput(uiTextRepository.save(UiText.create(
                nextUiTextId.getKey(), value,
                nextUiTextId.getLanguage(), description
        )));
    }
}
