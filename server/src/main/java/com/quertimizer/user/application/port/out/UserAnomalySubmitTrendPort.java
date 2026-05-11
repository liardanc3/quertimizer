package com.quertimizer.user.application.port.out;

import com.quertimizer.user.domain.model.UserAnomalySubmitTrend;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface UserAnomalySubmitTrendPort {

    Page<UserAnomalySubmitTrend> findUserSubmitCounts(Pageable pageable);

    Page<UserAnomalySubmitTrend> findUserSubmitCountsSince(LocalDateTime submittedAfter, Pageable pageable);

    Page<UserAnomalySubmitTrend> findUserSubmitCountsBetween(LocalDateTime submittedStart, LocalDateTime submittedEnd, Pageable pageable);

}
