package com.quertimizer.user.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileSolvedRecordsOutput {

    private final List<UserProfileSolvedRecordOutput> solvedRecords;
}
