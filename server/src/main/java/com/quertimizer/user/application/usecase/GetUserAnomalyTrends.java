package com.quertimizer.user.application.usecase;

import com.quertimizer.user.application.input.UserAnomalyTrendSearchInput;
import com.quertimizer.user.application.output.UserAnomalyTrendItemOutput;
import com.quertimizer.user.application.output.UserAnomalyTrendPageOutput;
import com.quertimizer.problem.application.port.ProblemSubmitHistoryRepository;
import com.quertimizer.user.domain.model.UserAnomalyAction;
import com.quertimizer.user.domain.model.UserAnomalyDateTimeConstant;
import com.quertimizer.user.domain.model.UserAnomalyPageConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static com.quertimizer.user.domain.model.UserAnomalyFailReason.CUSTOM_RANGE_FORMAT_INVALID;
import static com.quertimizer.user.domain.model.UserAnomalyFailReason.CUSTOM_RANGE_REQUIRED;
import static com.quertimizer.user.domain.model.UserAnomalyFailReason.START_AFTER_END;
import static com.quertimizer.user.domain.model.UserAnomalyFailReason.UNSUPPORTED_RANGE;
import static com.quertimizer.user.domain.model.UserAnomalyRangeBoundary.END;
import static com.quertimizer.user.domain.model.UserAnomalyRangeBoundary.START;

@Component
@RequiredArgsConstructor
public class GetUserAnomalyTrends {

    private final ProblemSubmitHistoryRepository problemSubmitHistoryRepository;

    /**
     * 이상 제출 추세를 조회한다.
     *
     * <ol>
     *   <li>요청 페이지와 페이지 크기 정규화
     *   <li>조회 범위에 맞는 제출 집계 조회
     *   <li>이상 제출 추세 페이지 응답 조립
     * </ol>
     *
     * @param input 이상 제출 추세 검색 입력
     */
    @Transactional(readOnly = true)
    public UserAnomalyTrendPageOutput execute(UserAnomalyTrendSearchInput input) {
        int currentPage = Math.max(1, input.getPage());
        int pageSize = normalizePageSize(input.getPageSize());
        Page<ProblemSubmitHistoryRepository.UserSubmitCountProjection> submitTrendPage = resolveSubmitTrendPage(
                input.getRange(), input.getStartedAt(), input.getEndedAt(), currentPage, pageSize
        );

        return new UserAnomalyTrendPageOutput(
                currentPage, pageSize, submitTrendPage.getTotalElements(), Math.max(1, submitTrendPage.getTotalPages()),
                submitTrendPage.getContent().stream()
                        .map(projection -> new UserAnomalyTrendItemOutput(
                                projection.getHandle(), UserAnomalyAction.SUBMIT.getLabel(), projection.getSubmitCount()
                        ))
                        .toList()
        );
    }

    private Page<ProblemSubmitHistoryRepository.UserSubmitCountProjection> resolveSubmitTrendPage(String range,
                                                                                                  String startedAt,
                                                                                                  String endedAt,
                                                                                                  int currentPage,
                                                                                                  int pageSize) {
        // 조회 범위와 페이지 요청 준비
        Pageable pageable = PageRequest.of(currentPage - 1, pageSize);
        String normalizedRange = range == null ? "10m" : range.trim();

        // 전체 범위면 전체 제출 집계 조회
        if ("all".equals(normalizedRange)) {
            return problemSubmitHistoryRepository.findUserSubmitCounts(pageable);
        }

        // 사용자 지정 범위면 시작/종료 시각 검증 후 제출 집계 조회
        if ("custom".equals(normalizedRange)) {
            LocalDateTime submittedStart = parseCustomRangeValue(startedAt, START.getLabel());
            LocalDateTime submittedEnd = parseCustomRangeValue(endedAt, END.getLabel());
            if (submittedStart.isAfter(submittedEnd)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, START_AFTER_END.getMessage());
            }

            return problemSubmitHistoryRepository.findUserSubmitCountsBetween(submittedStart, submittedEnd, pageable);
        }

        // 프리셋 범위 기준 제출 집계 조회
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
        // 사용자 지정 조회 시간 필수값 검사
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, CUSTOM_RANGE_REQUIRED.format(label));
        }

        // 사용자 지정 조회 시간 파싱
        try {
            return LocalDateTime.parse(value.trim(), UserAnomalyDateTimeConstant.CUSTOM_RANGE_FORMATTER);
        } catch (DateTimeParseException ignored) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, CUSTOM_RANGE_FORMAT_INVALID.format(label));
        }
    }

    private int normalizePageSize(Integer requestedPageSize) {
        // 요청 페이지 크기 없으면 기본 크기 반환
        if (requestedPageSize == null) {
            return UserAnomalyPageConstant.DEFAULT_PAGE_SIZE;
        }

        // 요청 페이지 크기를 허용 범위로 보정
        return Math.min(UserAnomalyPageConstant.MAX_PAGE_SIZE, Math.max(1, requestedPageSize));
    }
}
