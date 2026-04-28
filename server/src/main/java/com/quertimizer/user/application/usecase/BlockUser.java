package com.quertimizer.user.application.usecase;

import com.quertimizer.auth.application.service.AccountRestrictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlockUser {

    private final AccountRestrictionService accountRestrictionService;

    /**
     * 사용자를 차단한다.
     *
     * @param handle 차단할 사용자 handle
     */
    public void execute(String handle) {
        accountRestrictionService.blockUser(handle);
    }
}
