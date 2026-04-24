package com.quertimizer.alarm.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AlarmPageOutput {

    private final int currentPage;
    private final int pageSize;
    private final long totalCount;
    private final int totalPages;
    private final long unreadCount;
    private final List<AlarmItemOutput> alarms;
}
