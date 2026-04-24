package com.quertimizer.user.application.service;

import com.quertimizer.user.application.output.UserAnomalyTrendItemOutput;
import com.quertimizer.user.application.output.UserAnomalyTrendPageOutput;
import com.quertimizer.user.domain.model.UserAnomalyAction;
import com.quertimizer.problem.application.port.ProblemSubmitHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static com.quertimizer.user.domain.model.UserAnomalyFailReason.CUSTOM_RANGE_FORMAT_INVALID;
import static com.quertimizer.user.domain.model.UserAnomalyFailReason.CUSTOM_RANGE_REQUIRED;
import static com.quertimizer.user.domain.model.UserAnomalyFailReason.START_AFTER_END;
import static com.quertimizer.user.domain.model.UserAnomalyFailReason.UNSUPPORTED_RANGE;
import static com.quertimizer.user.domain.model.UserAnomalyRangeBoundary.END;
import static com.quertimizer.user.domain.model.UserAnomalyRangeBoundary.START;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAnomalyDetectionService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final DateTimeFormatter CUSTOM_RANGE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ProblemSubmitHistoryRepository problemSubmitHistoryRepository;

    public UserAnomalyTrendPageOutput getSubmitTrend(String range, String startedAt, String endedAt, int requestedPage, Integer requestedPageSize) {
        // 조회 페이지와 범위에 맞는 제출 이상 추세를 조회
        int currentPage = Math.max(1, requestedPage);
        int pageSize = normalizePageSize(requestedPageSize);
        Page<ProblemSubmitHistoryRepository.UserSubmitCountProjection> submitTrendPage = resolveSubmitTrendPage(range, startedAt, endedAt, currentPage, pageSize);

        return new UserAnomalyTrendPageOutput(
                currentPage,
                pageSize,
                submitTrendPage.getTotalElements(),
                Math.max(1, submitTrendPage.getTotalPages()),
                submitTrendPage.getContent().stream()
                        .map(projection -> new UserAnomalyTrendItemOutput(
                                projection.getHandle(),
                                UserAnomalyAction.SUBMIT.getLabel(),
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
        // 조회 범위에 맞는 제출 집계 페이지를 조회
        Pageable pageable = PageRequest.of(currentPage - 1, pageSize);
        String normalizedRange = range == null ? "10m" : range.trim();

        if ("all".equals(normalizedRange)) {
            return problemSubmitHistoryRepository.findUserSubmitCounts(pageable);
        }

        if ("custom".equals(normalizedRange)) {
            LocalDateTime submittedStart = parseCustomRangeValue(startedAt, START.getLabel());
            LocalDateTime submittedEnd = parseCustomRangeValue(endedAt, END.getLabel());
            if (submittedStart.isAfter(submittedEnd)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, START_AFTER_END.getMessage());
            }

            return problemSubmitHistoryRepository.findUserSubmitCountsBetween(submittedStart, submittedEnd, pageable);
        }

        return problemSubmitHistoryRepository.findUserSubmitCountsSince(resolveSubmittedAfter(normalizedRange), pageable);
    }

    private LocalDateTime resolveSubmittedAfter(String range) {
        // 프리셋 조회 범위를 기준 시간으로 변환
        return switch (range == null ? "10m" : range.trim()) {
            case "10m" -> LocalDateTime.now().minusMinutes(10);
            case "1h" -> LocalDateTime.now().minusHours(1);
            case "24h" -> LocalDateTime.now().minusHours(24);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, UNSUPPORTED_RANGE.getMessage());
        };
    }

    private LocalDateTime parseCustomRangeValue(String value, String label) {
        // 사용자 지정 조회 시간을 파싱
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, CUSTOM_RANGE_REQUIRED.format(label));
        }

        try {
            return LocalDateTime.parse(value.trim(), CUSTOM_RANGE_FORMATTER);
        } catch (DateTimeParseException ignored) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, CUSTOM_RANGE_FORMAT_INVALID.format(label));
        }
    }

    private int normalizePageSize(Integer requestedPageSize) {
        // 요청 페이지 크기를 허용 범위로 보정
        if (requestedPageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(MAX_PAGE_SIZE, Math.max(1, requestedPageSize));
    }
}
