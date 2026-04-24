package com.quertimizer.user.presentation.dto.response;

import com.quertimizer.user.application.output.UserProfileSolvedRecordsOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileSolvedRecordsRes {

    private final List<UserProfileSolvedRecordRes> solvedRecords;

    public static UserProfileSolvedRecordsRes from(UserProfileSolvedRecordsOutput result) {
        return new UserProfileSolvedRecordsRes(result.getSolvedRecords().stream()
                .map(UserProfileSolvedRecordRes::from)
                .toList());
    }
}
