package com.quertimizer.alarm.presentation.controller;

import com.quertimizer.alarm.presentation.dto.request.AlarmTemplateSaveReq;
import com.quertimizer.alarm.presentation.dto.response.AlarmTemplateRes;
import com.quertimizer.alarm.application.usecase.GetAdminAlarmTemplates;
import com.quertimizer.alarm.application.usecase.UpdateAlarmTemplate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AlarmTemplateController {

    private final GetAdminAlarmTemplates getAdminAlarmTemplates;
    private final UpdateAlarmTemplate updateAlarmTemplate;

    @GetMapping("/admin/alarm-templates")
    public ResponseEntity<List<AlarmTemplateRes>> getAdminAlarmTemplates() {
        // 관리자 알람 템플릿 목록을 조회
        return ResponseEntity.ok(getAdminAlarmTemplates.execute().stream()
                .map(AlarmTemplateRes::from)
                .toList());
    }

    @PutMapping("/admin/alarm-templates/{alarmType}")
    public ResponseEntity<AlarmTemplateRes> updateAlarmTemplate(@PathVariable String alarmType,
                                                                @Valid @RequestBody AlarmTemplateSaveReq request) {
        // 관리자 알람 템플릿을 수정
        return ResponseEntity.ok(AlarmTemplateRes.from(
                updateAlarmTemplate.execute(alarmType, request.toAlarmTemplateInput())
        ));
    }
}
