package com.quertimizer.ui.presentation.controller;

import com.quertimizer.ui.presentation.dto.request.UiTextSaveReq;
import com.quertimizer.ui.presentation.dto.response.UiTextPageRes;
import com.quertimizer.ui.presentation.dto.response.UiTextRes;
import com.quertimizer.ui.application.usecase.CreateUiText;
import com.quertimizer.ui.application.usecase.DeleteUiText;
import com.quertimizer.ui.application.usecase.GetAdminUiTexts;
import com.quertimizer.ui.application.usecase.GetUiText;
import com.quertimizer.ui.application.usecase.GetUiTexts;
import com.quertimizer.ui.application.usecase.UpdateUiText;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UiTextController {

    private final GetUiTexts getUiTexts;
    private final GetUiText getUiText;
    private final GetAdminUiTexts getAdminUiTexts;
    private final CreateUiText createUiText;
    private final UpdateUiText updateUiText;
    private final DeleteUiText deleteUiText;

    @GetMapping("/ui-texts")
    public ResponseEntity<List<UiTextRes>> getUiTexts(@RequestParam(defaultValue = "default") String language) {
        // UI 텍스트 전체 목록 조회
        return ResponseEntity.ok(getUiTexts.execute(language).stream()
                .map(UiTextRes::from)
                .toList());
    }

    @GetMapping("/ui-texts/{key}")
    public ResponseEntity<UiTextRes> getUiText(@PathVariable String key,
                                               @RequestParam(defaultValue = "default") String language) {
        // UI 텍스트 key, language 기준 조회
        return ResponseEntity.of(getUiText.execute(key, language).map(UiTextRes::from));
    }

    @GetMapping("/admin/ui-texts")
    public ResponseEntity<UiTextPageRes> getAdminUiTexts(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(required = false) Integer pageSize,
                                                         @RequestParam(required = false) String query) {
        // UI 텍스트 목록 조회
        return ResponseEntity.ok(UiTextPageRes.from(getAdminUiTexts.execute(page, pageSize, query)));
    }

    @PostMapping("/admin/ui-texts")
    public ResponseEntity<UiTextRes> createUiText(@Valid @RequestBody UiTextSaveReq request) {
        // UI 텍스트 생성
        return ResponseEntity.ok(UiTextRes.from(createUiText.execute(request.toUiTextInput())));
    }

    @PutMapping("/admin/ui-texts/{key}/{language}")
    public ResponseEntity<UiTextRes> updateUiText(@PathVariable String key,
                                                  @PathVariable String language,
                                                  @Valid @RequestBody UiTextSaveReq request) {
        // UI 텍스트 수정
        return ResponseEntity.ok(UiTextRes.from(updateUiText.execute(key, language, request.toUiTextInput())));
    }

    @DeleteMapping("/admin/ui-texts/{key}/{language}")
    public ResponseEntity<Void> deleteUiText(@PathVariable String key,
                                             @PathVariable String language) {
        // UI 텍스트 삭제
        deleteUiText.execute(key, language);
        return ResponseEntity.ok().build();
    }
}
