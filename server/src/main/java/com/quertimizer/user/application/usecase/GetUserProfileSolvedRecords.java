package com.quertimizer.user.application.usecase;

import com.quertimizer.user.application.output.UserProfileSolvedRecordsOutput;
import com.quertimizer.user.application.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserProfileSolvedRecords {

    private final UserProfileService userProfileService;

    public Optional<UserProfileSolvedRecordsOutput> execute(String targetHandle, String currentHandle) {
        // 프로필 제출 기록을 조회
        return userProfileService.getSolvedRecords(targetHandle, currentHandle);
    }
}
