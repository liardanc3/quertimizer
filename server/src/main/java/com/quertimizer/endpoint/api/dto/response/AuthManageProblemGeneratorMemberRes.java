package com.quertimizer.endpoint.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuthManageProblemGeneratorMemberRes {

    private final String userId;
    private final List<String> problemIds;

}
