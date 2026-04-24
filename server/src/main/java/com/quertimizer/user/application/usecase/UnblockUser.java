package com.quertimizer.user.application.usecase;

import com.quertimizer.auth.application.service.AccountRestrictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UnblockUser {

    private final AccountRestrictionService accountRestrictionService;

    public void execute(String handle) {
        // 사용자 차단을 해제
        accountRestrictionService.unblockUser(handle);
    }
}
