package com.quertimizer.endpoint.api.dto.request;

import com.quertimizer.constant.DbmsType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserProfileUpdateReq {

    @Size(max = 1000, message = "소개글은 최대 1000자까지 입력할 수 있습니다.")
    private String bio;

    @Valid
    @NotNull
    @Size(max = 10, message = "프로필 링크는 최대 10개까지 추가할 수 있습니다.")
    @Builder.Default
    private List<UserProfileLinkReq> links = new ArrayList<>();

    @NotNull(message = "기본 DBMS를 선택해 주세요.")
    private DbmsType defaultDbms;

    private boolean sqlPublic;

    private boolean executionPercentilePublic;

    private boolean solvedRecordsPublic;

    private boolean solvedProblemCountPublic;

}
