package com.quertimizer.alarm.application.service;

import com.quertimizer.alarm.application.port.in.GetAlarmsUseCase;
import com.quertimizer.alarm.application.input.AlarmPageInput;
import com.quertimizer.alarm.application.output.AlarmPageOutput;
import com.quertimizer.alarm.application.port.out.UserAlarmRepositoryPort;
import com.quertimizer.alarm.application.service.AlarmService;
import com.quertimizer.alarm.domain.entity.UserAlarm;
import com.quertimizer.alarm.domain.model.AlarmPageConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetAlarms implements GetAlarmsUseCase {

    private final UserAlarmRepositoryPort userAlarmRepository;
    private final AlarmService alarmService;

    /**
     * 사용자 알람 페이지를 조회한다.
     *
     * <ol>
     *   <li>요청 페이지와 페이지 크기 정규화
     *   <li>현재 페이지에 맞는 알람 조회
     *   <li>읽지 않은 알람 수와 항목 응답 조립
     * </ol>
     *
     * @param input 알람 페이지 조회 조건
     */
    @Transactional(readOnly = true)
    @Override
    public AlarmPageOutput execute(AlarmPageInput input) {
        int normalizedPage = Math.max(1, input.getPage());
        int pageSize = normalizePageSize(input.getPageSize());
        Page<UserAlarm> alarmPage = findAlarmPage(input.getHandle(), normalizedPage, pageSize, input.getCreatedAtSort());
        int totalPages = Math.max(1, alarmPage.getTotalPages());
        int currentPage = Math.min(normalizedPage, totalPages);

        if (currentPage != normalizedPage) {
            alarmPage = findAlarmPage(input.getHandle(), currentPage, pageSize, input.getCreatedAtSort());
        }

        return new AlarmPageOutput(
                currentPage, pageSize, alarmPage.getTotalElements(), Math.max(1, alarmPage.getTotalPages()),
                userAlarmRepository.countByHandleAndReadFalse(input.getHandle()),
                alarmPage.getContent().stream().map(alarmService::toAlarmItemOutput).toList()
        );
    }

    private Page<UserAlarm> findAlarmPage(String handle, int page, int pageSize, String createdAtSort) {
        // 정렬 방향 결정 후 알람 페이지 조회
        Sort.Direction direction = "asc".equalsIgnoreCase(createdAtSort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return userAlarmRepository.findAllByHandle(
                handle,
                PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(direction, "createdAt"), new Sort.Order(direction, "alarmId")))
        );
    }

    private int normalizePageSize(Integer requestedPageSize) {
        // 요청 페이지 크기 없으면 기본 크기 반환
        if (requestedPageSize == null) {
            return AlarmPageConstant.DEFAULT_PAGE_SIZE;
        }

        // 알람 페이지 크기 범위 보정
        return Math.min(AlarmPageConstant.MAX_PAGE_SIZE, Math.max(1, requestedPageSize));
    }
}
