package com.quertimizer.service;

import com.quertimizer.endpoint.api.dto.response.AdminAnomalyTrendItemRes;
import com.quertimizer.endpoint.api.dto.response.AdminAnomalyTrendPageRes;
import com.quertimizer.repository.ProblemSubmitHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAnomalyDetectionService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final String SUBMIT_ACTION_LABEL = "제출";
    private static final DateTimeFormatter CUSTOM_RANGE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ProblemSubmitHistoryRepository problemSubmitHistoryRepository;

    public AdminAnomalyTrendPageRes getSubmitTrend(String range, String startedAt, String endedAt, int requestedPage, Integer requestedPageSize) {
        int currentPage = Math.max(1, requestedPage);
        int pageSize = normalizePageSize(requestedPageSize);
        Page<ProblemSubmitHistoryRepository.UserSubmitCountProjection> submitTrendPage = resolveSubmitTrendPage(range, startedAt, endedAt, currentPage, pageSize);

        return new AdminAnomalyTrendPageRes(
                currentPage,
                pageSize,
                submitTrendPage.getTotalElements(),
                Math.max(1, submitTrendPage.getTotalPages()),
                submitTrendPage.getContent().stream()
                        .map(projection -> new AdminAnomalyTrendItemRes(
                                projection.getUserId(),
                                SUBMIT_ACTION_LABEL,
                                projection.getSubmitCount()
                        ))
                        .toList()
        );
    }

    private Page<ProblemSubmitHistoryRepository.UserSubmitCountProjection> resolveSubmitTrendPage(String range,
                                                                                                  String startedAt,
                                                                                                  String endedAt,
                                                                                                  int currentPage,
                                                                                                  int pageSize) {
        Pageable pageable = PageRequest.of(currentPage - 1, pageSize);
        String normalizedRange = range == null ? "10m" : range.trim();

        if ("all".equals(normalizedRange)) {
            return problemSubmitHistoryRepository.findUserSubmitCounts(pageable);
        }

        if ("custom".equals(normalizedRange)) {
            LocalDateTime submittedStart = parseCustomRangeValue(startedAt, "시작");
            LocalDateTime submittedEnd = parseCustomRangeValue(endedAt, "종료");
            if (submittedStart.isAfter(submittedEnd)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작 일시는 종료 일시보다 늦을 수 없습니다.");
            }

            return problemSubmitHistoryRepository.findUserSubmitCountsBetween(submittedStart, submittedEnd, pageable);
        }

        return problemSubmitHistoryRepository.findUserSubmitCountsSince(resolveSubmittedAfter(normalizedRange), pageable);
    }

    private LocalDateTime resolveSubmittedAfter(String range) {
        return switch (range == null ? "10m" : range.trim()) {
            case "10m" -> LocalDateTime.now().minusMinutes(10);
            case "1h" -> LocalDateTime.now().minusHours(1);
            case "24h" -> LocalDateTime.now().minusHours(24);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 조회 범위입니다.");
        };
    }

    private LocalDateTime parseCustomRangeValue(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " 일시를 입력해 주세요.");
        }

        try {
            return LocalDateTime.parse(value.trim(), CUSTOM_RANGE_FORMATTER);
        } catch (DateTimeParseException ignored) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " 일시 형식이 올바르지 않습니다.");
        }
    }

    private int normalizePageSize(Integer requestedPageSize) {
        if (requestedPageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(MAX_PAGE_SIZE, Math.max(1, requestedPageSize));
    }

}
