package com.quertimizer.admin.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuthManageProblemGeneratorGroupRes {

    private final int count;
    private final List<AuthManageProblemGeneratorMemberRes> members;

}
