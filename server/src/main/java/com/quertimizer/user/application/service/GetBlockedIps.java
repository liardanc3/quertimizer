package com.quertimizer.user.application.service;

import com.quertimizer.user.application.port.in.GetBlockedIpsUseCase;
import com.quertimizer.auth.application.port.out.BlockedIpRepositoryPort;
import com.quertimizer.auth.application.output.BlockedIpPageOutput;
import com.quertimizer.auth.application.service.AccountRestrictionService;
import com.quertimizer.auth.domain.entity.BlockedIp;
import com.quertimizer.user.application.input.BlockedAccountPageInput;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetBlockedIps implements GetBlockedIpsUseCase {

    private final BlockedIpRepositoryPort blockedIpRepository;
    private final AccountRestrictionService accountRestrictionService;

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
    public BlockedIpPageOutput execute(BlockedAccountPageInput input) {
        int currentPage = Math.max(1, input.getPage());
        int pageSize = accountRestrictionService.normalizePageSize(input.getPageSize());
        Page<BlockedIp> blockedIpPage = blockedIpRepository.findAllByOrderByBlockedAtDescIpAddressAsc(PageRequest.of(currentPage - 1, pageSize));
        return new BlockedIpPageOutput(
                currentPage, pageSize, blockedIpPage.getTotalElements(), Math.max(1, blockedIpPage.getTotalPages()),
                blockedIpPage.getContent().stream().map(accountRestrictionService::toBlockedIpItemOutput).toList()
        );
    }
}
