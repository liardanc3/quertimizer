package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.output.BlockedIpItemOutput;
import com.quertimizer.auth.application.output.BlockedUserItemOutput;
import com.quertimizer.auth.application.port.out.BlockedIpRepositoryPort;
import com.quertimizer.auth.domain.entity.BlockedIp;
import com.quertimizer.auth.domain.model.BlockedAccountPageConstant;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountRestrictionService {

    private final BlockedIpRepositoryPort blockedIpRepository;

    @Transactional(readOnly = true)
    public boolean isBlockedIp(String ipAddress) {
        // 공백이 아닌 IP만 차단 목록에서 확인
        return ipAddress != null && !ipAddress.isBlank() && blockedIpRepository.existsByIpAddress(ipAddress.trim());
    }

    public int normalizePageSize(Integer requestedPageSize) {
        // 페이지 크기 정규화
        if (requestedPageSize == null) {
            return BlockedAccountPageConstant.DEFAULT_PAGE_SIZE;
        }

        return Math.min(BlockedAccountPageConstant.MAX_PAGE_SIZE, Math.max(1, requestedPageSize));
    }

    public BlockedUserItemOutput toBlockedUserItemOutput(User user) {
        // 차단 사용자 항목 응답 변환
        return new BlockedUserItemOutput(user.getHandle(), user.getLastAccessIp(), user.getBlockedAt());
    }

    public BlockedIpItemOutput toBlockedIpItemOutput(BlockedIp blockedIp) {
        // 차단 IP 항목 응답 변환
        return new BlockedIpItemOutput(blockedIp.getIpAddress(), blockedIp.getBlockedAt());
    }
}
