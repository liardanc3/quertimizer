package com.quertimizer.user.presentation.dto.request;

import com.quertimizer.global.constant.DbmsType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static com.quertimizer.user.domain.model.UserValidationMessage.BIO_LENGTH_EXCEEDED;
import static com.quertimizer.user.domain.model.UserValidationMessage.DEFAULT_DBMS_REQUIRED;
import static com.quertimizer.user.domain.model.UserValidationMessage.PROFILE_LINK_LIMIT_EXCEEDED;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserProfileUpdateReq {

    @Size(max = 1000, message = BIO_LENGTH_EXCEEDED)
    private String bio;

    @Valid
    @NotNull
    @Size(max = 10, message = PROFILE_LINK_LIMIT_EXCEEDED)
    @Builder.Default
    private List<UserProfileLinkReq> links = new ArrayList<>();

    @NotNull(message = DEFAULT_DBMS_REQUIRED)
    private DbmsType defaultDbms;

    private boolean sqlPublic;

    private boolean executionPercentilePublic;

    private boolean solvedRecordsPublic;

    private boolean solvedProblemCountPublic;

}
