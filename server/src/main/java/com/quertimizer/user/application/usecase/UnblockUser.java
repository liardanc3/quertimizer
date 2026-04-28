package com.quertimizer.user.application.usecase;

import com.quertimizer.auth.application.service.AccountRestrictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UnblockUser {

    private final AccountRestrictionService accountRestrictionService;

    /**
     * 사용자 차단을 해제한다.
     *
     * @param handle 차단 해제할 사용자 handle
     */
    public void execute(String handle) {
        accountRestrictionService.unblockUser(handle);
    }
}
