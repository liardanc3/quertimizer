package com.quertimizer.user.adapter.out.auth;

import com.quertimizer.auth.application.input.AuthIpBlockInput;
import com.quertimizer.auth.application.port.in.BlockAuthIpUseCase;
import com.quertimizer.auth.application.port.in.GetAuthBlockedIpsUseCase;
import com.quertimizer.auth.application.port.in.UnblockAuthIpByHandleUseCase;
import com.quertimizer.auth.application.port.in.UnblockAuthIpUseCase;
import com.quertimizer.user.application.port.out.UserAccountRestrictionPort;
import com.quertimizer.user.domain.model.UserBlockedIp;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component("userAuthGateway")
@RequiredArgsConstructor
public class AuthGateway implements UserAccountRestrictionPort {

    private final BlockAuthIpUseCase blockAuthIp;
    private final UnblockAuthIpByHandleUseCase unblockAuthIpByHandle;
    private final UnblockAuthIpUseCase unblockAuthIp;
    private final GetAuthBlockedIpsUseCase getAuthBlockedIps;

    @Override
    public void blockIp(String ipAddress, String handle) {
        // auth 공개 use case 기준 IP 차단
        blockAuthIp.execute(new AuthIpBlockInput(ipAddress, handle));
    }

    @Override
    public void unblockHandle(String handle) {
        // auth 공개 use case 기준 handle 연결 IP 차단 해제
        unblockAuthIpByHandle.execute(handle);
    }

    @Override
    public void unblockIp(String ipAddress) {
        // auth 공개 use case 기준 IP 차단 해제
        unblockAuthIp.execute(ipAddress);
    }

    @Override
    public Page<UserBlockedIp> findBlockedIps(Pageable pageable) {
        // auth 공개 use case 기준 차단 IP 조회 결과를 user 조회 모델로 변환
        return getAuthBlockedIps.execute(pageable)
                .map(blockedIp -> new UserBlockedIp(blockedIp.getIpAddress(), blockedIp.getBlockedAt()));
    }
}
