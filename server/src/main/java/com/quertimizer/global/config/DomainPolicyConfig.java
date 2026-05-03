package com.quertimizer.global.config;

import com.quertimizer.auth.domain.policy.AuthManagePolicy;
import com.quertimizer.auth.domain.policy.AuthRateLimitPolicy;
import com.quertimizer.auth.domain.policy.LoginPolicy;
import com.quertimizer.auth.domain.policy.SignupPolicy;
import com.quertimizer.community.domain.policy.CommunityContentPolicy;
import com.quertimizer.community.domain.policy.CommunityNoticePolicy;
import com.quertimizer.community.domain.policy.CommunityViewPolicy;
import com.quertimizer.dashboard.domain.policy.DashboardHotPostPolicy;
import com.quertimizer.dashboard.domain.policy.DashboardProblemRecommendationPolicy;
import com.quertimizer.problem.domain.policy.ProblemAnswerPolicy;
import com.quertimizer.problem.domain.policy.ProblemExecutionPlanPolicy;
import com.quertimizer.problem.domain.policy.ProblemOfficialCostPolicy;
import com.quertimizer.problem.domain.policy.ProblemSolveHistoryPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainPolicyConfig {

    @Bean
    public SignupPolicy signupPolicy() {
        return new SignupPolicy();
    }

    @Bean
    public LoginPolicy loginPolicy() {
        return new LoginPolicy();
    }

    @Bean
    public AuthManagePolicy authManagePolicy() {
        return new AuthManagePolicy();
    }

    @Bean
    public AuthRateLimitPolicy authRateLimitPolicy() {
        return new AuthRateLimitPolicy();
    }

    @Bean
    public CommunityContentPolicy communityContentPolicy() {
        return new CommunityContentPolicy();
    }

    @Bean
    public CommunityNoticePolicy communityNoticePolicy() {
        return new CommunityNoticePolicy();
    }

    @Bean
    public CommunityViewPolicy communityViewPolicy() {
        return new CommunityViewPolicy();
    }

    @Bean
    public DashboardHotPostPolicy dashboardHotPostPolicy() {
        return new DashboardHotPostPolicy();
    }

    @Bean
    public DashboardProblemRecommendationPolicy dashboardProblemRecommendationPolicy() {
        return new DashboardProblemRecommendationPolicy();
    }

    @Bean
    public ProblemAnswerPolicy problemAnswerPolicy() {
        return new ProblemAnswerPolicy();
    }

    @Bean
    public ProblemExecutionPlanPolicy problemExecutionPlanPolicy() {
        return new ProblemExecutionPlanPolicy();
    }

    @Bean
    public ProblemOfficialCostPolicy problemOfficialCostPolicy() {
        return new ProblemOfficialCostPolicy();
    }

    @Bean
    public ProblemSolveHistoryPolicy problemSolveHistoryPolicy() {
        return new ProblemSolveHistoryPolicy();
    }
}
