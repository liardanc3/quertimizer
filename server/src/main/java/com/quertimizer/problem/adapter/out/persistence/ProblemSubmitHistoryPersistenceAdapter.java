package com.quertimizer.problem.adapter.out.persistence;

import com.quertimizer.problem.application.port.out.ProblemSubmitHistoryRepositoryPort;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProblemSubmitHistoryPersistenceAdapter implements ProblemSubmitHistoryRepositoryPort {

    private final ProblemSubmitHistoryJpaRepository problemSubmitHistoryJpaRepository;
    private final ProblemSubmitHistoryPersistenceMapper problemSubmitHistoryPersistenceMapper;

    @Override
    public List<ProblemSubmitHistory> findAll() {
        return problemSubmitHistoryJpaRepository.findAll().stream()
                .map(problemSubmitHistoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ProblemSubmitHistory> findAll(Sort sort) {
        return problemSubmitHistoryJpaRepository.findAll(sort).stream()
                .map(problemSubmitHistoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ProblemSubmitHistory> findAllByHandleOrderBySubmittedAtDesc(String handle) {
        return problemSubmitHistoryJpaRepository.findAllByHandleOrderBySubmittedAtDesc(handle).stream()
                .map(problemSubmitHistoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public ProblemSubmitHistory save(ProblemSubmitHistory problemSubmitHistory) {
        ProblemSubmitHistoryJpaEntity savedEntity = problemSubmitHistoryJpaRepository.save(
                problemSubmitHistoryPersistenceMapper.toEntity(problemSubmitHistory)
        );
        return problemSubmitHistoryPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Page<UserSubmitCountProjection> findUserSubmitCounts(Pageable pageable) {
        return problemSubmitHistoryJpaRepository.findUserSubmitCounts(pageable)
                .map(projection -> new UserSubmitCount(projection.getHandle(), projection.getSubmitCount()));
    }

    @Override
    public Page<UserSubmitCountProjection> findUserSubmitCountsSince(LocalDateTime submittedAfter, Pageable pageable) {
        return problemSubmitHistoryJpaRepository.findUserSubmitCountsSince(submittedAfter, pageable)
                .map(projection -> new UserSubmitCount(projection.getHandle(), projection.getSubmitCount()));
    }

    @Override
    public Page<UserSubmitCountProjection> findUserSubmitCountsBetween(LocalDateTime submittedStart,
                                                                       LocalDateTime submittedEnd,
                                                                       Pageable pageable) {
        return problemSubmitHistoryJpaRepository.findUserSubmitCountsBetween(submittedStart, submittedEnd, pageable)
                .map(projection -> new UserSubmitCount(projection.getHandle(), projection.getSubmitCount()));
    }

    private static final class UserSubmitCount implements UserSubmitCountProjection {
        private final String handle;
        private final long submitCount;

        private UserSubmitCount(String handle, long submitCount) {
            this.handle = handle;
            this.submitCount = submitCount;
        }

        @Override
        public String getHandle() {
            return handle;
        }

        @Override
        public long getSubmitCount() {
            return submitCount;
        }
    }
}
