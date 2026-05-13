package com.quertimizer.alarm.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.alarm.application.port.in.SendAdminAlarmUseCase;
import com.quertimizer.alarm.application.input.SendAdminAlarmInput;
import com.quertimizer.alarm.application.port.out.AlarmRecipientPort;
import com.quertimizer.alarm.domain.model.AdminDirectAlarm;
import com.quertimizer.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.quertimizer.alarm.domain.model.AlarmFailReason.HANDLE_NOT_FOUND;
import static com.quertimizer.alarm.domain.model.AlarmFailReason.MESSAGE_REQUIRED;
import static com.quertimizer.alarm.domain.model.AlarmFailReason.RECIPIENT_REQUIRED;

@Component
@RequiredArgsConstructor
public class SendAdminAlarm implements SendAdminAlarmUseCase {

    private final AlarmRecipientPort alarmRecipientPort;
    private final AlarmService alarmService;

    /**
     * 관리자 공지 알람을 전송한다.
     *
     * <ol>
     *   <li>수신자와 메시지 정규화
     *   <li>수신자 handle 존재 검증
     *   <li>관리자 직접 알람 발행
     * </ol>
     *
     * @param input 관리자 알람 수신자와 메시지
     */
    @Transactional
    @Override
    @Log("관리자 알람 전송")
    public int execute(SendAdminAlarmInput input) {
        List<String> normalizedRecipientHandles = normalizeRecipientHandles(input.getRecipientHandles());
        String normalizedMessage = requireMessage(input.getMessage());
        List<String> resolvedRecipientHandles = alarmRecipientPort.findExistingHandles(normalizedRecipientHandles);

        if (resolvedRecipientHandles.size() != normalizedRecipientHandles.size()) {
            throw new BusinessException(HANDLE_NOT_FOUND.getMessage(), HttpStatus.BAD_REQUEST);
        }

        normalizedRecipientHandles.forEach(recipientHandle -> alarmService.publish(new AdminDirectAlarm(recipientHandle, normalizedMessage)));
        return normalizedRecipientHandles.size();
    }

    private List<String> normalizeRecipientHandles(List<String> recipientHandles) {
        // 수신 handle 목록 존재 여부 검사
        if (recipientHandles == null) {
            throw new BusinessException(RECIPIENT_REQUIRED.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // 수신 handle 목록 공백 제거와 중복 제거
        List<String> normalizedRecipientHandles = recipientHandles.stream()
                .map(handle -> handle == null ? "" : handle.trim())
                .filter(handle -> !handle.isBlank())
                .distinct()
                .toList();

        // 유효 수신 handle 존재 여부 검사
        if (normalizedRecipientHandles.isEmpty()) {
            throw new BusinessException(RECIPIENT_REQUIRED.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return normalizedRecipientHandles;
    }

    private String requireMessage(String message) {
        // 메시지 필수값 검증
        if (message == null || message.isBlank()) {
            throw new BusinessException(MESSAGE_REQUIRED.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return message.trim();
    }
}
