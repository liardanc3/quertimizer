package com.quertimizer.ui.presentation.controller;

import com.quertimizer.ui.application.input.AdminUiTextSearchInput;
import com.quertimizer.ui.application.input.UiTextKeyInput;
import com.quertimizer.ui.application.input.UpdateUiTextInput;
import com.quertimizer.ui.application.usecase.CreateUiText;
import com.quertimizer.ui.application.usecase.DeleteUiText;
import com.quertimizer.ui.application.usecase.GetAdminUiTexts;
import com.quertimizer.ui.application.usecase.GetUiText;
import com.quertimizer.ui.application.usecase.GetUiTexts;
import com.quertimizer.ui.application.usecase.UpdateUiText;
import com.quertimizer.ui.presentation.dto.request.UiTextSaveReq;
import com.quertimizer.ui.presentation.dto.response.UiTextPageRes;
import com.quertimizer.ui.presentation.dto.response.UiTextRes;
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

    /**
     * 지정 언어의 UI 텍스트 전체 목록을 반환한다.
     *
     * @param language 조회할 UI 텍스트 언어
     */
    @GetMapping("/ui-texts")
    public ResponseEntity<List<UiTextRes>> getUiTexts(@RequestParam(defaultValue = "default") String language) {
        return ResponseEntity.ok(getUiTexts.execute(language).stream()
                .map(UiTextRes::from)
                .toList());
    }

    /**
     * key와 언어가 일치하는 UI 텍스트를 반환한다.
     *
     * @param key 조회할 UI 텍스트 key
     * @param language 조회할 UI 텍스트 언어
     */
    @GetMapping("/ui-texts/{key}")
    public ResponseEntity<UiTextRes> getUiText(@PathVariable String key,
                                               @RequestParam(defaultValue = "default") String language) {
        return ResponseEntity.of(getUiText.execute(new UiTextKeyInput(key, language)).map(UiTextRes::from));
    }

    /**
     * 관리자 UI 텍스트 페이지를 반환한다.
     *
     * @param page 요청 페이지 번호
     * @param pageSize 요청 페이지 크기
     * @param query UI 텍스트 검색어
     */
    @GetMapping("/admin/ui-texts")
    public ResponseEntity<UiTextPageRes> getAdminUiTexts(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(required = false) Integer pageSize,
                                                         @RequestParam(required = false) String query) {
        AdminUiTextSearchInput input = new AdminUiTextSearchInput(page, pageSize, query);
        return ResponseEntity.ok(UiTextPageRes.from(getAdminUiTexts.execute(input)));
    }

    /**
     * 관리자 UI 텍스트를 생성한다.
     *
     * @param request 생성할 UI 텍스트 요청
     */
    @PostMapping("/admin/ui-texts")
    public ResponseEntity<UiTextRes> createUiText(@Valid @RequestBody UiTextSaveReq request) {
        return ResponseEntity.ok(UiTextRes.from(createUiText.execute(request.toUiTextInput())));
    }

    /**
     * 관리자 UI 텍스트를 수정한다.
     *
     * @param key 수정할 UI 텍스트 key
     * @param language 수정할 UI 텍스트 언어
     * @param request 저장할 UI 텍스트 요청
     */
    @PutMapping("/admin/ui-texts/{key}/{language}")
    public ResponseEntity<UiTextRes> updateUiText(@PathVariable String key,
                                                  @PathVariable String language,
                                                  @Valid @RequestBody UiTextSaveReq request) {
        UpdateUiTextInput input = new UpdateUiTextInput(key, language, request.toUiTextInput());
        return ResponseEntity.ok(UiTextRes.from(updateUiText.execute(input)));
    }

    /**
     * 관리자 UI 텍스트를 삭제한다.
     *
     * @param key 삭제할 UI 텍스트 key
     * @param language 삭제할 UI 텍스트 언어
     */
    @DeleteMapping("/admin/ui-texts/{key}/{language}")
    public ResponseEntity<Void> deleteUiText(@PathVariable String key,
                                             @PathVariable String language) {
        deleteUiText.execute(new UiTextKeyInput(key, language));
        return ResponseEntity.ok().build();
    }
}
