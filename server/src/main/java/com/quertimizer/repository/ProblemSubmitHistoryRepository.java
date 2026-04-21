package com.quertimizer.repository;

import com.quertimizer.entity.ProblemSubmitHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ProblemSubmitHistoryRepository extends JpaRepository<ProblemSubmitHistory, Long> {

    @Query(value = """
            select history.userId as userId, count(history) as submitCount
            from ProblemSubmitHistory history
            group by history.userId
            order by count(history) desc, history.userId asc
            """,
            countQuery = """
                    select count(distinct history.userId)
                    from ProblemSubmitHistory history
                    """)
    Page<UserSubmitCountProjection> findUserSubmitCounts(Pageable pageable);

    @Query(value = """
            select history.userId as userId, count(history) as submitCount
            from ProblemSubmitHistory history
            where history.submittedAt >= :submittedAfter
            group by history.userId
            order by count(history) desc, history.userId asc
            """,
            countQuery = """
                    select count(distinct history.userId)
                    from ProblemSubmitHistory history
                    where history.submittedAt >= :submittedAfter
                    """)
    Page<UserSubmitCountProjection> findUserSubmitCountsSince(@Param("submittedAfter") LocalDateTime submittedAfter, Pageable pageable);

    @Query(value = """
            select history.userId as userId, count(history) as submitCount
            from ProblemSubmitHistory history
            where history.submittedAt between :submittedStart and :submittedEnd
            group by history.userId
            order by count(history) desc, history.userId asc
            """,
            countQuery = """
                    select count(distinct history.userId)
                    from ProblemSubmitHistory history
                    where history.submittedAt between :submittedStart and :submittedEnd
                    """)
    Page<UserSubmitCountProjection> findUserSubmitCountsBetween(@Param("submittedStart") LocalDateTime submittedStart,
                                                                @Param("submittedEnd") LocalDateTime submittedEnd,
                                                                Pageable pageable);

    interface UserSubmitCountProjection {
        String getUserId();

        long getSubmitCount();
    }

}
