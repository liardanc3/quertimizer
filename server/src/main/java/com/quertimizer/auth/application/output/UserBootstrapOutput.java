package com.quertimizer.auth.application.output;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.global.constant.UserRole;
import lombok.Data;

@Data
public class UserBootstrapOutput {

    private final boolean authenticated;
    private final String handle;
    private final DbmsType defaultDbms;
    private final UserRole role;
    private final boolean handleSetupRequired;

    public static UserBootstrapOutput authenticated(String handle, DbmsType defaultDbms, UserRole role, boolean handleSetupRequired) {
        // 인증 상태 출력 생성
        return new UserBootstrapOutput(true, handle, defaultDbms, role, handleSetupRequired);
    }

    public static UserBootstrapOutput unauthenticated() {
        // 비인증 상태 출력 생성
        return new UserBootstrapOutput(false, null, null, null, false);
    }
}
