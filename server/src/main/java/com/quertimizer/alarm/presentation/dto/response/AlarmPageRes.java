package com.quertimizer.alarm.presentation.dto.response;

import com.quertimizer.alarm.application.output.AlarmPageOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AlarmPageRes {

    private final int currentPage;
    private final int pageSize;
    private final long totalCount;
    private final int totalPages;
    private final long unreadCount;
    private final List<AlarmItemRes> alarms;

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
