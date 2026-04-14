package com.quertimizer.endpoint.api.controller;

import com.quertimizer.constant.MarqueeConstant;
import com.quertimizer.service.MarqueeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MarqueeController {

    private final MarqueeService marqueeService;

    @GetMapping("/marquee")
    public ResponseEntity<MarqueeMessagesRes> getVisibleMarquees(Authentication authentication) {

        // 현재 사용자 기준 전광판 문구 조회
        return ResponseEntity.ok(marqueeService.getVisibleMarquees(authentication));
    }

    @GetMapping("/admin/marquees")
    public ResponseEntity<MarqueeManageRes> getAdminMarquees() {

        // 전광판 관리 목록 조회
        return ResponseEntity.ok(marqueeService.getAdminMarquees());
    }

    @PostMapping("/admin/marquees")
    public ResponseEntity<MarqueeItemRes> createMarquee(@Valid @RequestBody MarqueeSaveReq request) {

        // 전광판 생성
        return ResponseEntity.ok(marqueeService.createMarquee(request));
    }

    @PutMapping("/admin/marquees/{marqueeId}")
    public ResponseEntity<MarqueeItemRes> updateMarquee(@PathVariable Long marqueeId,
                                                        @Valid @RequestBody MarqueeSaveReq request) {

        // 전광판 수정
        return ResponseEntity.ok(marqueeService.updateMarquee(marqueeId, request));
    }

    @DeleteMapping("/admin/marquees/{marqueeId}")
    public ResponseEntity<Void> deleteMarquee(@PathVariable Long marqueeId) {

        // 전광판 삭제
        marqueeService.deleteMarquee(marqueeId);
        return ResponseEntity.ok().build();
    }

    public record MarqueeMessagesRes(List<String> messages) {
    }

    public record MarqueeManageRes(List<MarqueeItemRes> items) {
    }

    public record MarqueeItemRes(Long marqueeId,
                                 List<String> targets,
                                 String message,
                                 String mode,
                                 String startedAt,
                                 Integer repeatCount,
                                 String schedulePattern,
                                 String scheduleTime,
                                 boolean active) {
    }

    public record MarqueeSaveReq(
            @NotEmpty(message = MarqueeConstant.TARGET_REQUIRED_MESSAGE) List<String> targets,
            @NotBlank(message = MarqueeConstant.MESSAGE_REQUIRED_MESSAGE) String message,
            @NotBlank(message = MarqueeConstant.MODE_REQUIRED_MESSAGE) String mode,
            String startedAt,
            Integer repeatCount,
            String schedulePattern,
            String scheduleTime
    ) {
    }

}
