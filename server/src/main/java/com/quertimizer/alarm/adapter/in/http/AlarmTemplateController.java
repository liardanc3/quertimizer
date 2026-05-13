package com.quertimizer.alarm.adapter.in.http;

import com.quertimizer.alarm.application.port.in.GetAdminAlarmTemplatesUseCase;
import com.quertimizer.alarm.application.port.in.UpdateAlarmTemplateUseCase;
import com.quertimizer.alarm.adapter.in.http.request.AlarmTemplateSaveReq;
import com.quertimizer.alarm.adapter.in.http.response.AlarmTemplateRes;
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

    private final GetAdminAlarmTemplatesUseCase getAdminAlarmTemplates;
    private final UpdateAlarmTemplateUseCase updateAlarmTemplate;

    /**
     * 관리자 알람 템플릿 목록을 반환한다.
     */
    @GetMapping("/admin/alarm-templates")
    public ResponseEntity<List<AlarmTemplateRes>> getAdminAlarmTemplates() {
        return ResponseEntity.ok(getAdminAlarmTemplates.execute().stream()
                .map(AlarmTemplateRes::from)
                .toList());
    }

    /**
     * 관리자 알람 템플릿 내용을 수정한다.
     *
     * @param alarmType 수정할 알람 유형
     * @param request 저장할 템플릿 요청
     */
    @PutMapping("/admin/alarm-templates/{alarmType}")
    public ResponseEntity<AlarmTemplateRes> updateAlarmTemplate(@PathVariable String alarmType,
                                                                @Valid @RequestBody AlarmTemplateSaveReq request) {
        return ResponseEntity.ok(AlarmTemplateRes.from(
                updateAlarmTemplate.execute(request.toAlarmTemplateInput(alarmType))
        ));
    }
}
