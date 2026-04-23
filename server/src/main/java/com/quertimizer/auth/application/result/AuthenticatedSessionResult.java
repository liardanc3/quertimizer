package com.quertimizer.auth.application.result;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.global.constant.UserRole;

public record AuthenticatedSessionResult(boolean authenticated,
                                         String handle,
                                         DbmsType defaultDbms,
                                         UserRole role,
                                         boolean handleSetupRequired) {

    public static AuthenticatedSessionResult authenticated(String handle,
                                                           DbmsType defaultDbms,
                                                           UserRole role,
                                                           boolean handleSetupRequired) {
        return new AuthenticatedSessionResult(true, handle, defaultDbms, role, handleSetupRequired);
    }

    public static AuthenticatedSessionResult unauthenticated() {
        return new AuthenticatedSessionResult(false, null, null, null, false);
    }

}
