package com.quertimizer.endpoint.api.controller;

import com.quertimizer.endpoint.api.dto.request.AlarmTemplateSaveReq;
import com.quertimizer.endpoint.api.dto.response.AlarmTemplateRes;
import com.quertimizer.service.AlarmTemplateService;
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

    private final AlarmTemplateService alarmTemplateService;

    @GetMapping("/admin/alarm-templates")
    public ResponseEntity<List<AlarmTemplateRes>> getAdminAlarmTemplates() {

        return ResponseEntity.ok(alarmTemplateService.getAdminAlarmTemplates());
    }

    @PutMapping("/admin/alarm-templates/{alarmType}")
    public ResponseEntity<AlarmTemplateRes> updateAlarmTemplate(@PathVariable String alarmType,
                                                                @Valid @RequestBody AlarmTemplateSaveReq request) {

        return ResponseEntity.ok(alarmTemplateService.updateAlarmTemplate(alarmType, request));
    }

}
