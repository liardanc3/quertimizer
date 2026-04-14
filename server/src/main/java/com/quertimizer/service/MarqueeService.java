package com.quertimizer.service;

import com.quertimizer.constant.MarqueeConstant;
import com.quertimizer.endpoint.api.controller.MarqueeController;
import com.quertimizer.exception.BusinessException;
import com.quertimizer.repository.MarqueeRespository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarqueeService {

    private final MarqueeRespository marqueeRespository;

    public MarqueeController.MarqueeMessagesRes getVisibleMarquees(Authentication authentication) {
        String viewerTarget = resolveViewerTarget(authentication);
        LocalDateTime now = LocalDateTime.now();

        List<String> messages = marqueeRespository.findAll().stream()
                .filter(marqueeDocument -> isVisibleMarquee(marqueeDocument, viewerTarget, now))
                .sorted(Comparator.comparing(MarqueeRespository.MarqueeDocument::marqueeId))
                .map(MarqueeRespository.MarqueeDocument::message)
                .toList();

        return new MarqueeController.MarqueeMessagesRes(messages);
    }

    public MarqueeController.MarqueeManageRes getAdminMarquees() {
        LocalDateTime now = LocalDateTime.now();

        List<MarqueeController.MarqueeItemRes> items = marqueeRespository.findAll().stream()
                .sorted(Comparator.comparing(MarqueeRespository.MarqueeDocument::marqueeId).reversed())
                .map(marqueeDocument -> createMarqueeItemResponse(marqueeDocument, now))
                .toList();

        return new MarqueeController.MarqueeManageRes(items);
    }

    @Transactional
    public MarqueeController.MarqueeItemRes createMarquee(MarqueeController.MarqueeSaveReq request) {
        return createMarqueeItemResponse(marqueeRespository.save(createMarqueeDocument(null, request)), LocalDateTime.now());
    }

    @Transactional
    public MarqueeController.MarqueeItemRes updateMarquee(Long marqueeId, MarqueeController.MarqueeSaveReq request) {
        marqueeRespository.findById(marqueeId)
                .orElseThrow(() -> new BusinessException(MarqueeConstant.MARQUEE_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));

        return createMarqueeItemResponse(marqueeRespository.save(createMarqueeDocument(marqueeId, request)), LocalDateTime.now());
    }

    @Transactional
    public void deleteMarquee(Long marqueeId) {
        marqueeRespository.findById(marqueeId)
                .orElseThrow(() -> new BusinessException(MarqueeConstant.MARQUEE_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));

        marqueeRespository.delete(marqueeId);
    }

    private MarqueeController.MarqueeItemRes createMarqueeItemResponse(MarqueeRespository.MarqueeDocument marqueeDocument,
                                                                       LocalDateTime now) {
        return new MarqueeController.MarqueeItemRes(
                marqueeDocument.marqueeId(),
                marqueeDocument.targets(),
                marqueeDocument.message(),
                marqueeDocument.mode(),
                Optional.ofNullable(marqueeDocument.startedAt())
                        .map(LocalDateTime::toString)
                        .orElse(null),
                marqueeDocument.repeatCount(),
                marqueeDocument.schedulePattern(),
                Optional.ofNullable(marqueeDocument.scheduleTime())
                        .map(LocalTime::toString)
                        .orElse(null),
                isVisibleMarqueeForAnyone(marqueeDocument, now)
        );
    }

    private MarqueeRespository.MarqueeDocument createMarqueeDocument(Long marqueeId, MarqueeController.MarqueeSaveReq request) {
        String mode = normalizeMode(request.mode());
        List<String> targets = normalizeTargets(request.targets());
        String message = normalizeMessage(request.message());
        LocalDateTime startedAt = null;
        Integer repeatCount = null;
        String schedulePattern = null;
        LocalTime scheduleTime = null;

        if (MarqueeConstant.MODE_REPEAT.equals(mode)) {
            startedAt = parseStartedAt(request.startedAt());
            repeatCount = normalizeRepeatCount(request.repeatCount());
        }

        if (MarqueeConstant.MODE_SCHEDULE.equals(mode)) {
            schedulePattern = normalizeSchedulePattern(request.schedulePattern());
            scheduleTime = parseScheduleTime(request.scheduleTime(), schedulePattern);
        }

        return new MarqueeRespository.MarqueeDocument(
                marqueeId,
                targets,
                message,
                mode,
                startedAt,
                repeatCount,
                schedulePattern,
                scheduleTime,
                LocalDateTime.now()
        );
    }

    private List<String> normalizeTargets(List<String> targets) {
        if (targets == null || targets.isEmpty()) {
            throw new BusinessException(MarqueeConstant.TARGET_REQUIRED_MESSAGE, HttpStatus.BAD_REQUEST);
        }

        List<String> normalizedTargets = targets.stream()
                .map(target -> Optional.ofNullable(target)
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .orElseThrow(() -> new BusinessException(MarqueeConstant.INVALID_TARGET_MESSAGE, HttpStatus.BAD_REQUEST)))
                .map(target -> target.equals(MarqueeConstant.TARGET_PROBLEM_GENERATOR)
                        ? target
                        : target.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();

        if (!MarqueeConstant.AVAILABLE_TARGETS.containsAll(normalizedTargets)) {
            throw new BusinessException(MarqueeConstant.INVALID_TARGET_MESSAGE, HttpStatus.BAD_REQUEST);
        }

        if (normalizedTargets.contains(MarqueeConstant.TARGET_ALL)) {
            return List.of(MarqueeConstant.TARGET_ALL);
        }

        return normalizedTargets;
    }

    private String normalizeMessage(String message) {
        return Optional.ofNullable(message)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElseThrow(() -> new BusinessException(MarqueeConstant.MESSAGE_REQUIRED_MESSAGE, HttpStatus.BAD_REQUEST));
    }

    private String normalizeMode(String mode) {
        String normalizedMode = Optional.ofNullable(mode)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessException(MarqueeConstant.MODE_REQUIRED_MESSAGE, HttpStatus.BAD_REQUEST));

        if (!MarqueeConstant.AVAILABLE_MODES.contains(normalizedMode)) {
            throw new BusinessException(MarqueeConstant.INVALID_MODE_MESSAGE, HttpStatus.BAD_REQUEST);
        }

        return normalizedMode;
    }

    private LocalDateTime parseStartedAt(String startedAt) {
        String normalizedStartedAt = Optional.ofNullable(startedAt)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElseThrow(() -> new BusinessException(MarqueeConstant.STARTED_AT_REQUIRED_MESSAGE, HttpStatus.BAD_REQUEST));

        try {
            return LocalDateTime.parse(normalizedStartedAt);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(MarqueeConstant.INVALID_STARTED_AT_MESSAGE, HttpStatus.BAD_REQUEST);
        }
    }

    private Integer normalizeRepeatCount(Integer repeatCount) {
        if (repeatCount == null) {
            throw new BusinessException(MarqueeConstant.REPEAT_COUNT_REQUIRED_MESSAGE, HttpStatus.BAD_REQUEST);
        }

        if (repeatCount < 1) {
            throw new BusinessException(MarqueeConstant.INVALID_REPEAT_COUNT_MESSAGE, HttpStatus.BAD_REQUEST);
        }

        return repeatCount;
    }

    private String normalizeSchedulePattern(String schedulePattern) {
        String normalizedSchedulePattern = Optional.ofNullable(schedulePattern)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessException(MarqueeConstant.SCHEDULE_PATTERN_REQUIRED_MESSAGE, HttpStatus.BAD_REQUEST));

        if (!MarqueeConstant.AVAILABLE_SCHEDULE_PATTERNS.contains(normalizedSchedulePattern)) {
            throw new BusinessException(MarqueeConstant.INVALID_SCHEDULE_PATTERN_MESSAGE, HttpStatus.BAD_REQUEST);
        }

        return normalizedSchedulePattern;
    }

    private LocalTime parseScheduleTime(String scheduleTime, String schedulePattern) {
        if (MarqueeConstant.SCHEDULE_ALWAYS.equals(schedulePattern)) {
            return LocalTime.MIDNIGHT;
        }

        String normalizedScheduleTime = Optional.ofNullable(scheduleTime)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElseThrow(() -> new BusinessException(MarqueeConstant.SCHEDULE_TIME_REQUIRED_MESSAGE, HttpStatus.BAD_REQUEST));

        try {
            return LocalTime.parse(normalizedScheduleTime);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(MarqueeConstant.INVALID_SCHEDULE_TIME_MESSAGE, HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isVisibleMarquee(MarqueeRespository.MarqueeDocument marqueeDocument, String viewerTarget, LocalDateTime now) {
        return matchesTarget(marqueeDocument.targets(), viewerTarget)
                && matchesTimeCondition(marqueeDocument, now);
    }

    private boolean isVisibleMarqueeForAnyone(MarqueeRespository.MarqueeDocument marqueeDocument, LocalDateTime now) {
        if (marqueeDocument.targets().contains(MarqueeConstant.TARGET_ALL)) {
            return matchesTimeCondition(marqueeDocument, now);
        }

        return marqueeDocument.targets().stream()
                .anyMatch(target -> isVisibleMarquee(marqueeDocument, target, now));
    }

    private boolean matchesTarget(List<String> targets, String viewerTarget) {
        return targets.contains(MarqueeConstant.TARGET_ALL) || targets.contains(viewerTarget);
    }

    private boolean matchesTimeCondition(MarqueeRespository.MarqueeDocument marqueeDocument, LocalDateTime now) {
        return switch (marqueeDocument.mode()) {
            case MarqueeConstant.MODE_REPEAT -> matchesRepeatCondition(marqueeDocument, now);
            case MarqueeConstant.MODE_SCHEDULE -> matchesScheduleCondition(marqueeDocument, now);
            default -> false;
        };
    }

    private boolean matchesRepeatCondition(MarqueeRespository.MarqueeDocument marqueeDocument, LocalDateTime now) {
        if (marqueeDocument.startedAt() == null || marqueeDocument.repeatCount() == null) {
            return false;
        }

        LocalDateTime expiredAt = marqueeDocument.startedAt()
                .plusSeconds((long) marqueeDocument.repeatCount() * MarqueeConstant.MARQUEE_LOOP_SECONDS);

        return !now.isBefore(marqueeDocument.startedAt()) && now.isBefore(expiredAt);
    }

    private boolean matchesScheduleCondition(MarqueeRespository.MarqueeDocument marqueeDocument, LocalDateTime now) {
        String schedulePattern = Optional.ofNullable(marqueeDocument.schedulePattern()).orElse("");

        if (MarqueeConstant.SCHEDULE_ALWAYS.equals(schedulePattern)) {
            return true;
        }

        if (marqueeDocument.scheduleTime() == null || now.toLocalTime().isBefore(marqueeDocument.scheduleTime())) {
            return false;
        }

        DayOfWeek dayOfWeek = now.getDayOfWeek();
        return switch (schedulePattern) {
            case MarqueeConstant.SCHEDULE_DAILY -> true;
            case MarqueeConstant.SCHEDULE_WEEKDAYS -> dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
            case MarqueeConstant.SCHEDULE_WEEKEND -> dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
            default -> false;
        };
    }

    private String resolveViewerTarget(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return MarqueeConstant.TARGET_GUEST;
        }

        List<String> authorities = AuthorityUtils.authorityListToSet(authentication.getAuthorities()).stream().toList();
        if (authorities.contains("ROLE_ADMIN")) {
            return MarqueeConstant.TARGET_ADMIN;
        }

        if (authorities.contains("ROLE_PROBLEM_GENERATOR")) {
            return MarqueeConstant.TARGET_PROBLEM_GENERATOR;
        }

        return MarqueeConstant.TARGET_USER;
    }

}
