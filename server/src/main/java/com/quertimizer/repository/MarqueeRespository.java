package com.quertimizer.repository;

import com.quertimizer.constant.MarqueeConstant;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MarqueeRespository {

    private final AtomicLong sequence = new AtomicLong(0L);
    private final Map<Long, MarqueeDocument> marqueeById = new ConcurrentHashMap<>();

    @PostConstruct
    public void seed() {
        if (!marqueeById.isEmpty()) {
            return;
        }

        save(new MarqueeDocument(
                null,
                List.of(MarqueeConstant.TARGET_ALL),
                MarqueeConstant.DEFAULT_MESSAGE,
                MarqueeConstant.MODE_SCHEDULE,
                null,
                null,
                MarqueeConstant.SCHEDULE_ALWAYS,
                LocalTime.MIDNIGHT,
                LocalDateTime.now()
        ));
    }

    public List<MarqueeDocument> findAll() {
        return marqueeById.values().stream()
                .sorted(Comparator.comparing(MarqueeDocument::marqueeId))
                .toList();
    }

    public Optional<MarqueeDocument> findById(Long marqueeId) {
        return Optional.ofNullable(marqueeById.get(marqueeId));
    }

    public MarqueeDocument save(MarqueeDocument marqueeDocument) {
        long marqueeId = marqueeDocument.marqueeId() != null
                ? marqueeDocument.marqueeId()
                : sequence.incrementAndGet();

        MarqueeDocument savedDocument = marqueeDocument.withMarqueeId(marqueeId);
        marqueeById.put(marqueeId, savedDocument);
        return savedDocument;
    }

    public void delete(Long marqueeId) {
        marqueeById.remove(marqueeId);
    }

    public record MarqueeDocument(Long marqueeId,
                                  List<String> targets,
                                  String message,
                                  String mode,
                                  LocalDateTime startedAt,
                                  Integer repeatCount,
                                  String schedulePattern,
                                  LocalTime scheduleTime,
                                  LocalDateTime updatedAt) {

        public MarqueeDocument {
            targets = List.copyOf(targets);
        }

        public MarqueeDocument withMarqueeId(Long nextMarqueeId) {
            return new MarqueeDocument(
                    nextMarqueeId,
                    targets,
                    message,
                    mode,
                    startedAt,
                    repeatCount,
                    schedulePattern,
                    scheduleTime,
                    updatedAt
            );
        }
    }

}
