package com.quertimizer.user.application.usecase;

import com.quertimizer.auth.application.service.AccountRestrictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlockUser {

    private final AccountRestrictionService accountRestrictionService;

    public void execute(String handle) {
        // 사용자를 차단
        accountRestrictionService.blockUser(handle);
    }
}
