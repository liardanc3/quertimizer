package com.quertimizer.alarm.adapter.in.web.response;

import com.quertimizer.alarm.application.output.AlarmPageOutput;
import com.quertimizer.alarm.domain.model.AlarmPageConstant;
import lombok.Data;

import java.util.List;

@Data
public class AlarmPageRes {

    private final int currentPage;
    private final int pageSize;
    private final long totalCount;
    private final int totalPages;
    private final long unreadCount;
    private final List<AlarmItemRes> alarms;

    public static AlarmPageRes empty(int page, Integer pageSize) {
        // 빈 알람 페이지 크기 보정
        int normalizedPageSize = pageSize == null
                ? AlarmPageConstant.DEFAULT_PAGE_SIZE
                : Math.min(AlarmPageConstant.MAX_PAGE_SIZE, Math.max(1, pageSize));

        // 빈 알람 페이지 응답 반환
        return new AlarmPageRes(Math.max(1, page), normalizedPageSize, 0, 1, 0, List.of());
    }

    public static AlarmPageRes from(AlarmPageOutput result) {
        return new AlarmPageRes(
                result.getCurrentPage(),
                result.getPageSize(),
                result.getTotalCount(),
                result.getTotalPages(),
                result.getUnreadCount(),
                result.getAlarms().stream()
                        .map(AlarmItemRes::from)
                        .toList()
        );
    }
}
