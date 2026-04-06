package com.quertimizer.endpoint.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileSolvedRecordsRes {

    private final List<UserProfileSolvedRecordRes> solvedRecords;

}
