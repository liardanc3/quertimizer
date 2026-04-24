package com.quertimizer.user.application.input;

import com.quertimizer.global.constant.DbmsType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileUpdateInput {

    private final String bio;
    private final List<UserProfileLinkInput> links;
    private final DbmsType defaultDbms;
    private final boolean sqlPublic;
    private final boolean executionPercentilePublic;
    private final boolean solvedRecordsPublic;
    private final boolean solvedProblemCountPublic;
}
