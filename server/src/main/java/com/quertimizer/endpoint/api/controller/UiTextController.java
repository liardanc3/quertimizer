package com.quertimizer.endpoint.api.controller;

import com.quertimizer.endpoint.api.dto.request.UiTextSaveReq;
import com.quertimizer.endpoint.api.dto.response.UiTextRes;
import com.quertimizer.service.UiTextService;
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

    private final UiTextService uiTextService;

    @GetMapping("/ui-texts")
    public ResponseEntity<List<UiTextRes>> getUiTexts(@RequestParam(defaultValue = "default") String language) {

        // UI 텍스트 전체 목록 조회
        return ResponseEntity.ok(uiTextService.getUiTexts(language));
    }

    @GetMapping("/ui-texts/{key}")
    public ResponseEntity<UiTextRes> getUiText(@PathVariable String key,
                                               @RequestParam(defaultValue = "default") String language) {

        // UI 텍스트 key, language 기준 조회
        return ResponseEntity.of(uiTextService.getUiText(key, language));
    }

    @GetMapping("/admin/ui-texts")
    public ResponseEntity<List<UiTextRes>> getAdminUiTexts() {

        // UI 텍스트 목록 조회
        return ResponseEntity.ok(uiTextService.getAdminUiTexts());
    }

    @PostMapping("/admin/ui-texts")
    public ResponseEntity<UiTextRes> createUiText(@Valid @RequestBody UiTextSaveReq request) {

        // UI 텍스트 생성
        return ResponseEntity.ok(uiTextService.createUiText(request));
    }

    @PutMapping("/admin/ui-texts/{key}/{language}")
    public ResponseEntity<UiTextRes> updateUiText(@PathVariable String key,
                                                  @PathVariable String language,
                                                  @Valid @RequestBody UiTextSaveReq request) {

        // UI 텍스트 수정
        return ResponseEntity.ok(uiTextService.updateUiText(key, language, request));
    }

    @DeleteMapping("/admin/ui-texts/{key}/{language}")
    public ResponseEntity<Void> deleteUiText(@PathVariable String key,
                                             @PathVariable String language) {

        // UI 텍스트 삭제
        uiTextService.deleteUiText(key, language);
        return ResponseEntity.ok().build();
    }

}
