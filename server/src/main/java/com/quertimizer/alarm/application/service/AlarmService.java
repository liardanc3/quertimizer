package com.quertimizer.alarm.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.alarm.application.output.AlarmCreatedOutput;
import com.quertimizer.alarm.application.output.AlarmItemOutput;
import com.quertimizer.alarm.application.port.out.AlarmNotifierPort;
import com.quertimizer.alarm.application.port.out.UserAlarmRepositoryPort;
import com.quertimizer.alarm.domain.entity.AlarmTemplate;
import com.quertimizer.alarm.domain.entity.UserAlarm;
import com.quertimizer.alarm.domain.model.AlarmBinding;
import com.quertimizer.alarm.domain.model.AlarmSpec;
import com.quertimizer.alarm.domain.model.AlarmType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.quertimizer.alarm.domain.model.AlarmLogMessage.BINDING_DESERIALIZE_FAILED;
import static com.quertimizer.alarm.domain.model.AlarmLogMessage.BINDING_SERIALIZE_FAILED;
import static com.quertimizer.alarm.domain.model.AlarmLogMessage.SOCKET_SEND_FAILED;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AlarmService {

    private final UserAlarmRepositoryPort userAlarmRepository;
    private final AlarmNotifierPort alarmNotifier;
    private final AlarmTemplateService alarmTemplateService;
    private final ObjectMapper objectMapper;

    public void publish(AlarmSpec alarmSpec) {
        // 수신자 handle 없으면 알람 발행 생략
        if (alarmSpec.recipientHandle() == null || alarmSpec.recipientHandle().isBlank()) {
            return;
        }

        // 알람 저장 전 기본 템플릿 보장
        alarmTemplateService.ensureDefaultTemplates();

        // 알람 저장 후 미확인 알람 수 조회
        UserAlarm alarm = userAlarmRepository.save(UserAlarm.create(alarmSpec, serializeBindings(alarmSpec.bindings())));
        long unreadCount = userAlarmRepository.countByHandleAndReadFalse(alarm.getHandle());

        // 실시간 알림 발행 실패 시 로그 기록
        try {
            alarmNotifier.notifyCreated(alarm.getHandle(), AlarmCreatedOutput.created(toAlarmItemOutput(alarm), unreadCount));
        } catch (Exception exception) {
            log.warn(SOCKET_SEND_FAILED.getMessage(), exception);
        }
    }

    public AlarmItemOutput toAlarmItemOutput(UserAlarm alarm) {
        // 관리자 직접 알람이면 템플릿 없이 응답 변환
        if (AlarmType.FROM_ADMIN.getValue().equals(alarm.getAlarmType())) {
            return new AlarmItemOutput(
                    alarm.getAlarmId(), alarm.getAlarmType(), alarm.getTitle(), alarm.getMessage(),
                    "", "", Map.of(), alarm.getTargetPath(), alarm.getTargetHash(),
                    alarm.isRead(), alarm.getCreatedAt()
            );
        }

        // 알람 템플릿과 바인딩을 포함해 응답 변환
        AlarmTemplate alarmTemplate = alarmTemplateService.getAlarmTemplate(alarm.getAlarmType());
        return new AlarmItemOutput(
                alarm.getAlarmId(), alarmTemplate.getAlarmType(), alarm.getTitle(), alarm.getMessage(),
                alarmTemplate.getSentence(), alarmTemplate.getDescription(), deserializeBindings(alarm.getBindingsJson()),
                alarm.getTargetPath(), alarm.getTargetHash(), alarm.isRead(), alarm.getCreatedAt()
        );
    }

    private String serializeBindings(Map<String, AlarmBinding> bindings) {
        // 바인딩 직렬화
        try {
            return objectMapper.writeValueAsString(bindings);
        } catch (Exception exception) {
            log.warn(BINDING_SERIALIZE_FAILED.getMessage(), exception);
            return null;
        }
    }

    private Map<String, AlarmBinding> deserializeBindings(String bindingsJson) {
        // 바인딩 JSON 없으면 빈 바인딩 반환
        if (bindingsJson == null || bindingsJson.isBlank()) {
            return Map.of();
        }

        // 바인딩 JSON 역직렬화 실패 시 빈 바인딩 대체
        try {
            Map<String, Map<String, String>> rawBindings = objectMapper.readValue(
                    bindingsJson, new TypeReference<Map<String, Map<String, String>>>() {
                    }
            );
            Map<String, AlarmBinding> bindings = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, String>> entry : rawBindings.entrySet()) {
                Map<String, String> binding = entry.getValue();
                bindings.put(entry.getKey(), new AlarmBinding(binding.get("text"), binding.get("path"), binding.get("hash")));
            }

            return bindings;
        } catch (Exception exception) {
            log.warn(BINDING_DESERIALIZE_FAILED.getMessage(), exception);
            return Map.of();
        }
    }

}
