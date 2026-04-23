package com.quertimizer.alarm.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.alarm.domain.model.AdminDirectAlarm;
import com.quertimizer.alarm.domain.model.AlarmBinding;
import com.quertimizer.alarm.domain.model.AlarmSpec;
import com.quertimizer.alarm.domain.model.AlarmType;
import com.quertimizer.alarm.presentation.dto.response.AlarmItemRes;
import com.quertimizer.alarm.presentation.dto.response.AlarmPageRes;
import com.quertimizer.alarm.domain.entity.UserAlarm;
import com.quertimizer.alarm.presentation.realtime.dto.AlarmSocketRes;
import com.quertimizer.problem.presentation.realtime.handler.SessionWebSocketHandler;
import com.quertimizer.alarm.domain.entity.AlarmTemplate;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.alarm.infrastructure.repository.UserAlarmRepository;
import com.quertimizer.user.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final SessionWebSocketHandler sessionWebSocketHandler;
    private final AlarmTemplateService alarmTemplateService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public AlarmPageRes getAlarms(String handle, int requestedPage, Integer requestedPageSize) {
        int normalizedPage = Math.max(1, requestedPage);
        int pageSize = normalizePageSize(requestedPageSize);
        Page<UserAlarm> alarmPage = findAlarmPage(handle, normalizedPage, pageSize);
        int totalPages = Math.max(1, alarmPage.getTotalPages());
        int currentPage = Math.min(normalizedPage, totalPages);

        if (currentPage != normalizedPage) {
            alarmPage = findAlarmPage(handle, currentPage, pageSize);
        }

        return new AlarmPageRes(
                currentPage,
                pageSize,
                alarmPage.getTotalElements(),
                Math.max(1, alarmPage.getTotalPages()),
                userAlarmRepository.countByHandleAndReadFalse(handle),
                alarmPage.getContent().stream()
                        .map(this::toAlarmItemResponse)
                        .toList()
        );
    }

    public void markAllRead(String handle) {
        List<UserAlarm> unreadAlarms = userAlarmRepository.findAllByHandleAndReadFalseOrderByCreatedAtDescAlarmIdDesc(handle);

        if (unreadAlarms.isEmpty()) {
            return;
        }

        unreadAlarms.forEach(UserAlarm::markRead);
    }

    public boolean markRead(Long alarmId, String handle) {
        return userAlarmRepository.findByAlarmIdAndHandle(alarmId, handle)
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

    public int sendAdminAlarm(List<String> recipientHandles, String message) {
        List<String> normalizedRecipientHandles = normalizeRecipientHandles(recipientHandles);
        String normalizedMessage = requireMessage(message);
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
        if (alarmSpec.recipientHandle() == null || alarmSpec.recipientHandle().isBlank()) {
            return;
        }

        UserAlarm alarm = userAlarmRepository.save(UserAlarm.create(alarmSpec, serializeBindings(alarmSpec.bindings())));
        long unreadCount = userAlarmRepository.countByHandleAndReadFalse(alarm.getHandle());

        try {
            sessionWebSocketHandler.sendAlarm(alarm.getHandle(), AlarmSocketRes.created(toAlarmItemResponse(alarm), unreadCount));
        } catch (Exception exception) {
            log.warn(SOCKET_SEND_FAILED.getMessage(), exception);
        }
    }

    private Page<UserAlarm> findAlarmPage(String handle, int page, int pageSize) {
        return userAlarmRepository.findAllByHandleOrderByCreatedAtDescAlarmIdDesc(
                handle,
                PageRequest.of(
                        page - 1,
                        pageSize,
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("alarmId"))
                )
        );
    }

    private AlarmItemRes toAlarmItemResponse(UserAlarm alarm) {
        if (AlarmType.FROM_ADMIN.getValue().equals(alarm.getAlarmType())) {
            return new AlarmItemRes(
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

        return new AlarmItemRes(
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
        if (requestedPageSize == null) {
            return DEFAULT_ALARM_PAGE_SIZE;
        }

        return Math.min(MAX_ALARM_PAGE_SIZE, Math.max(1, requestedPageSize));
    }

    private String serializeBindings(Map<String, AlarmBinding> bindings) {
        try {
            return objectMapper.writeValueAsString(bindings);
        } catch (Exception exception) {
            log.warn(BINDING_SERIALIZE_FAILED.getMessage(), exception);
            return null;
        }
    }

    private List<String> normalizeRecipientHandles(List<String> recipientHandles) {
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
        if (message == null || message.isBlank()) {
            throw new BusinessException(MESSAGE_REQUIRED.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return message.trim();
    }

    private Map<String, AlarmBinding> deserializeBindings(String bindingsJson) {
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
