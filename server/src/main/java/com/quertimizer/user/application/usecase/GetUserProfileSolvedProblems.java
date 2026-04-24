package com.quertimizer.user.application.usecase;

import com.quertimizer.user.application.output.UserProfileSolvedProblemsOutput;
import com.quertimizer.user.application.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserProfileSolvedProblems {

    private final UserProfileService userProfileService;

    public Optional<UserProfileSolvedProblemsOutput> execute(String targetHandle, String currentHandle) {
        // 프로필 해결한 문제 목록을 조회
        return userProfileService.getSolvedProblems(targetHandle, currentHandle);
    }
}
