package com.quertimizer.user.application.output;

import lombok.Data;

import java.util.List;

@Data
public class UserProfileSolvedRecordsOutput {

    private final List<UserProfileSolvedRecordOutput> solvedRecords;
}
