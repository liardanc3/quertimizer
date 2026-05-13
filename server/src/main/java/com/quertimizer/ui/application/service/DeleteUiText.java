package com.quertimizer.ui.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.ui.application.port.in.DeleteUiTextUseCase;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.ui.application.input.UiTextKeyInput;
import com.quertimizer.ui.application.port.out.UiTextRepositoryPort;
import com.quertimizer.ui.domain.entity.UiText;
import com.quertimizer.ui.domain.entity.ids.UiTextId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.quertimizer.ui.domain.model.UiTextFailReason.UI_TEXT_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class DeleteUiText implements DeleteUiTextUseCase {

    private final UiTextRepositoryPort uiTextRepository;
    private final UiTextService uiTextService;

    /**
     * UI 텍스트를 삭제한다.
     *
     * @param input 삭제할 UI 텍스트 key와 언어 입력
     */
    @Transactional
    @Override
    @Log("UI 텍스트 삭제")
    public void execute(UiTextKeyInput input) {
        UiTextId uiTextId = uiTextService.createRequiredUiTextId(input.getKey(), input.getLanguage());
        UiText uiText = uiTextRepository.findById(uiTextId)
                .orElseThrow(() -> new BusinessException(UI_TEXT_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));

        uiTextRepository.delete(uiText);
    }
}
