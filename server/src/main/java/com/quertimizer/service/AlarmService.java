package com.quertimizer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.alarm.AdminDirectAlarm;
import com.quertimizer.alarm.AlarmBinding;
import com.quertimizer.alarm.AlarmSpec;
import com.quertimizer.endpoint.api.dto.response.AlarmItemRes;
import com.quertimizer.endpoint.api.dto.response.AlarmPageRes;
import com.quertimizer.endpoint.websocket.dto.AlarmSocketRes;
import com.quertimizer.endpoint.websocket.handler.SessionWebSocketHandler;
import com.quertimizer.entity.AlarmTemplate;
import com.quertimizer.entity.User;
import com.quertimizer.entity.UserAlarm;
import com.quertimizer.exception.BusinessException;
import com.quertimizer.repository.UserAlarmRepository;
import com.quertimizer.repository.UserRepository;
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

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AlarmService {

    private static final int DEFAULT_ALARM_PAGE_SIZE = 5;
    private static final int MAX_ALARM_PAGE_SIZE = 50;
    private static final String ADMIN_DIRECT_ALARM_TYPE = "FROM_ADMIN";
    private static final String RECIPIENT_REQUIRED_MESSAGE = "수신자가 필요하다.";
    private static final String MESSAGE_REQUIRED_MESSAGE = "알람 내용이 필요하다.";
    private static final String HANDLE_NOT_FOUND_MESSAGE = "존재하지 않는 Handle이 포함되어 있다.";

    private final UserAlarmRepository userAlarmRepository;
    private final UserRepository userRepository;
    private final SessionWebSocketHandler sessionWebSocketHandler;
    private final AlarmTemplateService alarmTemplateService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public AlarmPageRes getAlarms(String userId, int requestedPage, Integer requestedPageSize) {
        int normalizedPage = Math.max(1, requestedPage);
        int pageSize = normalizePageSize(requestedPageSize);
        Page<UserAlarm> alarmPage = findAlarmPage(userId, normalizedPage, pageSize);
        int totalPages = Math.max(1, alarmPage.getTotalPages());
        int currentPage = Math.min(normalizedPage, totalPages);

        if (currentPage != normalizedPage) {
            alarmPage = findAlarmPage(userId, currentPage, pageSize);
        }

        return new AlarmPageRes(
                currentPage,
                pageSize,
                alarmPage.getTotalElements(),
                Math.max(1, alarmPage.getTotalPages()),
                userAlarmRepository.countByUserIdAndReadFalse(userId),
                alarmPage.getContent().stream()
                        .map(this::toAlarmItemResponse)
                        .toList()
        );
    }

    public void markAllRead(String userId) {
        List<UserAlarm> unreadAlarms = userAlarmRepository.findAllByUserIdAndReadFalseOrderByCreatedAtDescAlarmIdDesc(userId);

        if (unreadAlarms.isEmpty()) {
            return;
        }

        unreadAlarms.forEach(UserAlarm::markRead);
    }

    public boolean markRead(Long alarmId, String userId) {
        return userAlarmRepository.findByAlarmIdAndUserId(alarmId, userId)
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

        return userRepository.findTop20ByUserIdContainingIgnoreCaseOrderByUserIdAsc(normalizedKeyword).stream()
                .map(User::getUserId)
                .filter(userId -> userId != null && !userId.isBlank())
                .distinct()
                .toList();
    }

    public int sendAdminAlarm(List<String> recipientHandles, String message) {
        List<String> normalizedRecipientHandles = normalizeRecipientHandles(recipientHandles);
        String normalizedMessage = requireMessage(message);
        List<User> recipientUsers = userRepository.findAllByUserIdIn(normalizedRecipientHandles);
        List<String> resolvedRecipientHandles = recipientUsers.stream()
                .map(User::getUserId)
                .filter(userId -> userId != null && !userId.isBlank())
                .distinct()
                .toList();

        if (resolvedRecipientHandles.size() != normalizedRecipientHandles.size()) {
            throw new BusinessException(HANDLE_NOT_FOUND_MESSAGE, HttpStatus.BAD_REQUEST);
        }

        normalizedRecipientHandles.forEach(recipientHandle -> publish(new AdminDirectAlarm(recipientHandle, normalizedMessage)));
        return normalizedRecipientHandles.size();
    }

    public void publish(AlarmSpec alarmSpec) {
        if (alarmSpec.recipientUserId() == null || alarmSpec.recipientUserId().isBlank()) {
            return;
        }

        UserAlarm alarm = userAlarmRepository.save(UserAlarm.create(alarmSpec, serializeBindings(alarmSpec.bindings())));
        long unreadCount = userAlarmRepository.countByUserIdAndReadFalse(alarm.getUserId());

        try {
            sessionWebSocketHandler.sendAlarm(alarm.getUserId(), AlarmSocketRes.created(toAlarmItemResponse(alarm), unreadCount));
        } catch (Exception exception) {
            log.warn("알람 소켓 전송에 실패했다.", exception);
        }
    }

    private Page<UserAlarm> findAlarmPage(String userId, int page, int pageSize) {
        return userAlarmRepository.findAllByUserIdOrderByCreatedAtDescAlarmIdDesc(
                userId,
                PageRequest.of(
                        page - 1,
                        pageSize,
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("alarmId"))
                )
        );
    }

    private AlarmItemRes toAlarmItemResponse(UserAlarm alarm) {
        if (ADMIN_DIRECT_ALARM_TYPE.equals(alarm.getAlarmType())) {
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
            log.warn("알람 바인딩 직렬화에 실패했다.", exception);
            return null;
        }
    }

    private List<String> normalizeRecipientHandles(List<String> recipientHandles) {
        if (recipientHandles == null) {
            throw new BusinessException(RECIPIENT_REQUIRED_MESSAGE, HttpStatus.BAD_REQUEST);
        }

        List<String> normalizedRecipientHandles = recipientHandles.stream()
                .map(handle -> handle == null ? "" : handle.trim())
                .filter(handle -> !handle.isBlank())
                .distinct()
                .toList();

        if (normalizedRecipientHandles.isEmpty()) {
            throw new BusinessException(RECIPIENT_REQUIRED_MESSAGE, HttpStatus.BAD_REQUEST);
        }

        return normalizedRecipientHandles;
    }

    private String requireMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new BusinessException(MESSAGE_REQUIRED_MESSAGE, HttpStatus.BAD_REQUEST);
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
            log.warn("알람 바인딩 역직렬화에 실패했다.", exception);
            return Map.of();
        }
    }

}
