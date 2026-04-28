package com.quertimizer.alarm.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.alarm.application.input.AlarmPageInput;
import com.quertimizer.alarm.application.input.MarkAlarmReadInput;
import com.quertimizer.alarm.application.input.SendAdminAlarmInput;
import com.quertimizer.alarm.application.output.AlarmCreatedOutput;
import com.quertimizer.alarm.application.output.AlarmItemOutput;
import com.quertimizer.alarm.application.output.AlarmPageOutput;
import com.quertimizer.alarm.application.port.AlarmNotifier;
import com.quertimizer.alarm.application.port.UserAlarmRepository;
import com.quertimizer.alarm.domain.entity.AlarmTemplate;
import com.quertimizer.alarm.domain.entity.UserAlarm;
import com.quertimizer.alarm.domain.model.AdminDirectAlarm;
import com.quertimizer.alarm.domain.model.AlarmBinding;
import com.quertimizer.alarm.domain.model.AlarmSpec;
import com.quertimizer.alarm.domain.model.AlarmType;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.user.application.port.UserRepository;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.quertimizer.alarm.domain.model.AlarmFailReason.HANDLE_NOT_FOUND;
import static com.quertimizer.alarm.domain.model.AlarmFailReason.MESSAGE_REQUIRED;
import static com.quertimizer.alarm.domain.model.AlarmFailReason.RECIPIENT_REQUIRED;
import static com.quertimizer.alarm.domain.model.AlarmLogMessage.BINDING_DESERIALIZE_FAILED;
import static com.quertimizer.alarm.domain.model.AlarmLogMessage.BINDING_SERIALIZE_FAILED;
import static com.quertimizer.alarm.domain.model.AlarmLogMessage.SOCKET_SEND_FAILED;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AlarmService {

    private static final int DEFAULT_ALARM_PAGE_SIZE = 5;
    private static final int MAX_ALARM_PAGE_SIZE = 50;

    private final UserAlarmRepository userAlarmRepository;
    private final UserRepository userRepository;
    private final AlarmNotifier alarmNotifier;
    private final AlarmTemplateService alarmTemplateService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public AlarmPageOutput getAlarms(AlarmPageInput input) {
        // 사용자 알람 페이지를 조회
        int normalizedPage = Math.max(1, input.getPage());
        int pageSize = normalizePageSize(input.getPageSize());
        Page<UserAlarm> alarmPage = findAlarmPage(input.getHandle(), normalizedPage, pageSize, input.getCreatedAtSort());
        int totalPages = Math.max(1, alarmPage.getTotalPages());
        int currentPage = Math.min(normalizedPage, totalPages);

        if (currentPage != normalizedPage) {
            alarmPage = findAlarmPage(input.getHandle(), currentPage, pageSize, input.getCreatedAtSort());
        }

        return new AlarmPageOutput(
                currentPage,
                pageSize,
                alarmPage.getTotalElements(),
                Math.max(1, alarmPage.getTotalPages()),
                userAlarmRepository.countByHandleAndReadFalse(input.getHandle()),
                alarmPage.getContent().stream()
                        .map(this::toAlarmItemOutput)
                        .toList()
        );
    }

    public void markAllRead(String handle) {
        // 사용자 알람을 모두 읽음 처리
        List<UserAlarm> unreadAlarms = userAlarmRepository.findAllByHandleAndReadFalseOrderByCreatedAtDescAlarmIdDesc(handle);

        if (unreadAlarms.isEmpty()) {
            return;
        }

        unreadAlarms.forEach(UserAlarm::markRead);
    }

    public boolean markRead(MarkAlarmReadInput input) {
        // 단일 알람을 읽음 처리
        return userAlarmRepository.findByAlarmIdAndHandle(input.getAlarmId(), input.getHandle())
                .map(alarm -> {
                    if (!alarm.isRead()) {
                        alarm.markRead();
                    }

                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<String> searchRecipientHandles(String keyword) {
        // 관리자 알람 수신 Handle 후보를 조회
        String normalizedKeyword = keyword == null ? "" : keyword.trim();

        if (normalizedKeyword.isBlank()) {
            return List.of();
        }

        return userRepository.findTop20ByHandleContainingIgnoreCaseOrderByHandleAsc(normalizedKeyword).stream()
                .map(User::getHandle)
                .filter(handle -> handle != null && !handle.isBlank())
                .distinct()
                .toList();
    }

    public int sendAdminAlarm(SendAdminAlarmInput input) {
        // 관리자 공지 알람을 전송
        List<String> normalizedRecipientHandles = normalizeRecipientHandles(input.getRecipientHandles());
        String normalizedMessage = requireMessage(input.getMessage());
        List<User> recipientUsers = userRepository.findAllByHandleIn(normalizedRecipientHandles);
        List<String> resolvedRecipientHandles = recipientUsers.stream()
                .map(User::getHandle)
                .filter(handle -> handle != null && !handle.isBlank())
                .distinct()
                .toList();

        if (resolvedRecipientHandles.size() != normalizedRecipientHandles.size()) {
            throw new BusinessException(HANDLE_NOT_FOUND.getMessage(), HttpStatus.BAD_REQUEST);
        }

        normalizedRecipientHandles.forEach(recipientHandle -> publish(new AdminDirectAlarm(recipientHandle, normalizedMessage)));
        return normalizedRecipientHandles.size();
    }

    public void publish(AlarmSpec alarmSpec) {
        // 알람 명세를 저장하고 실시간 알림을 발행
        if (alarmSpec.recipientHandle() == null || alarmSpec.recipientHandle().isBlank()) {
            return;
        }

        UserAlarm alarm = userAlarmRepository.save(UserAlarm.create(alarmSpec, serializeBindings(alarmSpec.bindings())));
        long unreadCount = userAlarmRepository.countByHandleAndReadFalse(alarm.getHandle());

        try {
            alarmNotifier.notifyCreated(alarm.getHandle(), AlarmCreatedOutput.created(toAlarmItemOutput(alarm), unreadCount));
        } catch (Exception exception) {
            log.warn(SOCKET_SEND_FAILED.getMessage(), exception);
        }
    }

    private Page<UserAlarm> findAlarmPage(String handle, int page, int pageSize, String createdAtSort) {
        // 알람 페이지 조회
        Sort.Direction direction = "asc".equalsIgnoreCase(createdAtSort) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return userAlarmRepository.findAllByHandle(
                handle,
                PageRequest.of(
                        page - 1,
                        pageSize,
                        Sort.by(new Sort.Order(direction, "createdAt"), new Sort.Order(direction, "alarmId"))
                )
        );
    }

    private AlarmItemOutput toAlarmItemOutput(UserAlarm alarm) {
        // 알람 항목 응답으로 변환
        if (AlarmType.FROM_ADMIN.getValue().equals(alarm.getAlarmType())) {
            return new AlarmItemOutput(
                    alarm.getAlarmId(),
                    alarm.getAlarmType(),
                    alarm.getTitle(),
                    alarm.getMessage(),
                    "",
                    "",
                    Map.of(),
                    alarm.getTargetPath(),
                    alarm.getTargetHash(),
                    alarm.isRead(),
                    alarm.getCreatedAt()
            );
        }

        AlarmTemplate alarmTemplate = alarmTemplateService.getAlarmTemplate(alarm.getAlarmType());

        return new AlarmItemOutput(
                alarm.getAlarmId(),
                alarmTemplate.getAlarmType(),
                alarm.getTitle(),
                alarm.getMessage(),
                alarmTemplate.getSentence(),
                alarmTemplate.getDescription(),
                deserializeBindings(alarm.getBindingsJson()),
                alarm.getTargetPath(),
                alarm.getTargetHash(),
                alarm.isRead(),
                alarm.getCreatedAt()
        );
    }

    private int normalizePageSize(Integer requestedPageSize) {
        // 페이지 크기 정규화
        if (requestedPageSize == null) {
            return DEFAULT_ALARM_PAGE_SIZE;
        }

        return Math.min(MAX_ALARM_PAGE_SIZE, Math.max(1, requestedPageSize));
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

    private List<String> normalizeRecipientHandles(List<String> recipientHandles) {
        // 수신 Handle 목록 정규화
        if (recipientHandles == null) {
            throw new BusinessException(RECIPIENT_REQUIRED.getMessage(), HttpStatus.BAD_REQUEST);
        }

        List<String> normalizedRecipientHandles = recipientHandles.stream()
                .map(handle -> handle == null ? "" : handle.trim())
                .filter(handle -> !handle.isBlank())
                .distinct()
                .toList();

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

    private Map<String, AlarmBinding> deserializeBindings(String bindingsJson) {
        // 바인딩 역직렬화
        if (bindingsJson == null || bindingsJson.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(bindingsJson, new TypeReference<Map<String, AlarmBinding>>() {
            });
        } catch (Exception exception) {
            log.warn(BINDING_DESERIALIZE_FAILED.getMessage(), exception);
            return Map.of();
        }
    }

}
