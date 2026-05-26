package com.quertimizer.user.adapter.out.problem;

import com.quertimizer.problem.application.input.ProblemUserSubmitCountInput;
import com.quertimizer.problem.application.output.ProblemUserProfileOutput;
import com.quertimizer.problem.application.output.ProblemUserSolvedRecordOutput;
import com.quertimizer.problem.application.output.ProblemUserSubmissionActivityOutput;
import com.quertimizer.problem.application.output.ProblemUserSubmitCountOutput;
import com.quertimizer.problem.application.port.in.GetProblemUserProfileUseCase;
import com.quertimizer.problem.application.port.in.GetProblemUserSubmitCountsUseCase;
import com.quertimizer.user.application.output.UserProfileSolvedProblemsOutput;
import com.quertimizer.user.application.output.UserProfileSolvedRecordOutput;
import com.quertimizer.user.application.output.UserProfileSolvedRecordsOutput;
import com.quertimizer.user.application.output.UserProfileSubmissionActivityOutput;
import com.quertimizer.user.application.output.UserProfileSubmissionSummaryOutput;
import com.quertimizer.user.application.port.out.UserAnomalySubmitTrendPort;
import com.quertimizer.user.application.port.out.UserProfileProblemPort;
import com.quertimizer.user.domain.model.UserAnomalySubmitTrend;
import com.quertimizer.user.domain.model.UserProfileProblemSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("userProblemGateway")
@RequiredArgsConstructor
public class ProblemGateway implements UserProfileProblemPort, UserAnomalySubmitTrendPort {

    private final GetProblemUserProfileUseCase getProblemUserProfile;
    private final GetProblemUserSubmitCountsUseCase getProblemUserSubmitCounts;

    @Override
    public UserProfileProblemSummary getProblemSummary(String handle) {
        // problem 공개 use case 기준 사용자 문제 요약 변환
        ProblemUserProfileOutput profile = getProblemUserProfile.execute(handle);
        return new UserProfileProblemSummary(
                profile.getSolvedCount(), profile.getTotalExecutionTimeMs(),
                profile.getPostgresqlAverageExecutionPercentile(),
                profile.getMysqlAverageExecutionPercentile()
        );
    }

    @Override
    public UserProfileSolvedProblemsOutput getSolvedProblems(String handle) {
        // problem 공개 use case 기준 해결 문제 목록 변환
        ProblemUserProfileOutput profile = getProblemUserProfile.execute(handle);
        return new UserProfileSolvedProblemsOutput(profile.getSolvedProblemIds().size(), profile.getSolvedProblemIds());
    }

    @Override
    public UserProfileSolvedRecordsOutput getSolvedRecords(String handle) {
        // problem 공개 use case 기준 해결 기록 목록 변환
        return new UserProfileSolvedRecordsOutput(getProblemUserProfile.execute(handle).getSolvedRecords().stream()
                .map(this::toSolvedRecordOutput)
                .toList());
    }

    @Override
    public UserProfileSubmissionSummaryOutput getSubmissionSummary(String handle) {
        // problem 공개 use case 기준 제출 요약 변환
        ProblemUserProfileOutput profile = getProblemUserProfile.execute(handle);
        return new UserProfileSubmissionSummaryOutput(
                profile.getAttemptedProblemIds(),
                profile.getSubmissionActivities().stream()
                        .map(this::toSubmissionActivityOutput)
                        .toList()
        );
    }

    @Override
    public Page<UserAnomalySubmitTrend> findUserSubmitCounts(Pageable pageable) {
        // problem 공개 use case 기준 제출 집계 변환
        return getProblemUserSubmitCounts.execute(new ProblemUserSubmitCountInput(null, null, pageable))
                .map(this::toSubmitTrend);
    }

    @Override
    public Page<UserAnomalySubmitTrend> findUserSubmitCountsSince(LocalDateTime submittedAfter, Pageable pageable) {
        // 기준 시간 이후 problem 공개 use case 기준 제출 집계 변환
        return getProblemUserSubmitCounts.execute(new ProblemUserSubmitCountInput(submittedAfter, null, pageable))
                .map(this::toSubmitTrend);
    }

    @Override
    public Page<UserAnomalySubmitTrend> findUserSubmitCountsBetween(LocalDateTime submittedStart,
                                                                    LocalDateTime submittedEnd,
                                                                    Pageable pageable) {
        // 기준 시간 범위 problem 공개 use case 기준 제출 집계 변환
        return getProblemUserSubmitCounts.execute(new ProblemUserSubmitCountInput(submittedStart, submittedEnd, pageable))
                .map(this::toSubmitTrend);
    }

    private UserProfileSolvedRecordOutput toSolvedRecordOutput(ProblemUserSolvedRecordOutput output) {
        // problem 해결 기록 응답을 user 프로필 응답으로 변환
        return new UserProfileSolvedRecordOutput(
                output.getProblemId(), output.getTitle(), output.getDbms(),
                output.getExecutionTimeMs(), output.getCost(), output.getSubmittedAt()
        );
    }

    private UserProfileSubmissionActivityOutput toSubmissionActivityOutput(ProblemUserSubmissionActivityOutput output) {
        // problem 제출 활동 응답을 user 프로필 응답으로 변환
        return new UserProfileSubmissionActivityOutput(output.getSubmittedDate(), output.getSubmitCount());
    }

    private UserAnomalySubmitTrend toSubmitTrend(ProblemUserSubmitCountOutput output) {
        // problem 제출 집계 응답을 user 이상 제출 추세 모델로 변환
        return new UserAnomalySubmitTrend(output.getHandle(), output.getSubmitCount());
    }
}
