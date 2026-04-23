package com.quertimizer.admin.application.service;

import com.quertimizer.admin.domain.model.AdminAnomalyAction;
import com.quertimizer.admin.presentation.dto.response.AdminAnomalyTrendItemRes;
import com.quertimizer.admin.presentation.dto.response.AdminAnomalyTrendPageRes;
import com.quertimizer.problem.infrastructure.repository.ProblemSubmitHistoryRepository;
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

import static com.quertimizer.admin.domain.model.AdminAnomalyRangeBoundary.END;
import static com.quertimizer.admin.domain.model.AdminAnomalyRangeBoundary.START;
import static com.quertimizer.admin.domain.model.AdminAnomalyFailReason.CUSTOM_RANGE_FORMAT_INVALID;
import static com.quertimizer.admin.domain.model.AdminAnomalyFailReason.CUSTOM_RANGE_REQUIRED;
import static com.quertimizer.admin.domain.model.AdminAnomalyFailReason.START_AFTER_END;
import static com.quertimizer.admin.domain.model.AdminAnomalyFailReason.UNSUPPORTED_RANGE;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAnomalyDetectionService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
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
                                projection.getHandle(),
                                AdminAnomalyAction.SUBMIT.getLabel(),
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
        return switch (range == null ? "10m" : range.trim()) {
            case "10m" -> LocalDateTime.now().minusMinutes(10);
            case "1h" -> LocalDateTime.now().minusHours(1);
            case "24h" -> LocalDateTime.now().minusHours(24);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, UNSUPPORTED_RANGE.getMessage());
        };
    }

    private LocalDateTime parseCustomRangeValue(String value, String label) {
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
        if (requestedPageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(MAX_PAGE_SIZE, Math.max(1, requestedPageSize));
    }

}
