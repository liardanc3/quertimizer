package com.quertimizer.user.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.user.application.input.BlockedAccountPageInput;
import com.quertimizer.user.application.output.BlockedIpItemOutput;
import com.quertimizer.user.application.output.BlockedIpPageOutput;
import com.quertimizer.user.application.port.in.GetBlockedIpsUseCase;
import com.quertimizer.user.application.port.out.UserAccountRestrictionPort;
import com.quertimizer.user.domain.model.UserBlockedIp;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.quertimizer.user.domain.model.UserBlockedAccountPageConstant.DEFAULT_PAGE_SIZE;
import static com.quertimizer.user.domain.model.UserBlockedAccountPageConstant.MAX_PAGE_SIZE;

@Component
@RequiredArgsConstructor
public class GetBlockedIps implements GetBlockedIpsUseCase {

    private final UserAccountRestrictionPort userAccountRestrictionPort;

    /**
     * 차단된 IP 목록을 조회한다.
     *
     * <ol>
     *   <li>요청 페이지와 페이지 크기 정규화
     *   <li>차단 IP 페이지 조회
     *   <li>차단 IP 페이지 응답 조립
     * </ol>
     *
     * @param input 차단 IP 페이지 조회 입력
     */
    @Transactional(readOnly = true)
    @Override
    @Log("차단 IP 목록 조회")
    public BlockedIpPageOutput execute(BlockedAccountPageInput input) {
        int currentPage = Math.max(1, input.getPage());
        int pageSize = normalizePageSize(input.getPageSize());
        Page<UserBlockedIp> blockedIpPage = userAccountRestrictionPort.findBlockedIps(PageRequest.of(currentPage - 1, pageSize));
        return new BlockedIpPageOutput(
                currentPage, pageSize, blockedIpPage.getTotalElements(), Math.max(1, blockedIpPage.getTotalPages()),
                blockedIpPage.getContent().stream()
                        .map(blockedIp -> new BlockedIpItemOutput(blockedIp.getIpAddress(), blockedIp.getBlockedAt()))
                        .toList()
        );
    }

    private int normalizePageSize(Integer requestedPageSize) {
        // 페이지 크기 정규화
        if (requestedPageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(MAX_PAGE_SIZE, Math.max(1, requestedPageSize));
    }
}
