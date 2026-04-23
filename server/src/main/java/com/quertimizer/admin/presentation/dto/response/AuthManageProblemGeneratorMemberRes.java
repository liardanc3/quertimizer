package com.quertimizer.admin.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuthManageProblemGeneratorMemberRes {

    private final String handle;
    private final List<String> problemIds;

}
