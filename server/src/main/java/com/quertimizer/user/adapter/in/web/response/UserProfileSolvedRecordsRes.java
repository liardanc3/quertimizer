package com.quertimizer.user.adapter.in.web.response;

import com.quertimizer.user.application.output.UserProfileSolvedRecordsOutput;
import lombok.Data;

import java.util.List;

@Data
public class UserProfileSolvedRecordsRes {

    private final List<UserProfileSolvedRecordRes> solvedRecords;

    public static UserProfileSolvedRecordsRes from(UserProfileSolvedRecordsOutput result) {
        return new UserProfileSolvedRecordsRes(result.getSolvedRecords().stream()
                .map(UserProfileSolvedRecordRes::from)
                .toList());
    }
}
