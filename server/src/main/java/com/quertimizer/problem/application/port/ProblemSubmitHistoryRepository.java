package com.quertimizer.problem.application.port;

import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

public interface ProblemSubmitHistoryRepository {

    List<ProblemSubmitHistory> findAll();

    List<ProblemSubmitHistory> findAll(Sort sort);

    List<ProblemSubmitHistory> findAllByHandleOrderBySubmittedAtDesc(String handle);

    <S extends ProblemSubmitHistory> S save(S problemSubmitHistory);

    Page<UserSubmitCountProjection> findUserSubmitCounts(Pageable pageable);

    Page<UserSubmitCountProjection> findUserSubmitCountsSince(LocalDateTime submittedAfter, Pageable pageable);

    Page<UserSubmitCountProjection> findUserSubmitCountsBetween(LocalDateTime submittedStart,
                                                                LocalDateTime submittedEnd,
                                                                Pageable pageable);

    interface UserSubmitCountProjection {
        String getHandle();

        long getSubmitCount();
    }
}
