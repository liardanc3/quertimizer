package com.quertimizer.user.application.usecase;

import com.quertimizer.auth.application.output.BlockedUserPageOutput;
import com.quertimizer.auth.application.service.AccountRestrictionService;
import com.quertimizer.user.application.input.BlockedAccountPageInput;
import com.quertimizer.user.application.port.UserRepository;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetBlockedUsers {

    private final UserRepository userRepository;
    private final AccountRestrictionService accountRestrictionService;

    /**
     * 차단된 사용자 목록을 조회한다.
     *
     * <ol>
     *   <li>요청 페이지와 페이지 크기 정규화
     *   <li>차단 사용자 페이지 조회
     *   <li>차단 사용자 페이지 응답 조립
     * </ol>
     *
     * @param input 차단 사용자 페이지 조회 입력
     */
    @Transactional(readOnly = true)
    public BlockedUserPageOutput execute(BlockedAccountPageInput input) {
        int currentPage = Math.max(1, input.getPage());
        int pageSize = accountRestrictionService.normalizePageSize(input.getPageSize());
        Page<User> blockedUserPage = userRepository.findAllByBlockedUserTrueOrderByBlockedAtDescHandleAsc(PageRequest.of(currentPage - 1, pageSize));
        return new BlockedUserPageOutput(
                currentPage, pageSize, blockedUserPage.getTotalElements(), Math.max(1, blockedUserPage.getTotalPages()),
                blockedUserPage.getContent().stream().map(accountRestrictionService::toBlockedUserItemOutput).toList()
        );
    }
}
