package com.quertimizer.user.application.service;

import com.quertimizer.user.application.input.BlockedAccountPageInput;
import com.quertimizer.user.application.output.BlockedUserItemOutput;
import com.quertimizer.user.application.output.BlockedUserPageOutput;
import com.quertimizer.user.application.port.in.GetBlockedUsersUseCase;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.quertimizer.user.domain.model.UserBlockedAccountPageConstant.DEFAULT_PAGE_SIZE;
import static com.quertimizer.user.domain.model.UserBlockedAccountPageConstant.MAX_PAGE_SIZE;

@Component
@RequiredArgsConstructor
public class GetBlockedUsers implements GetBlockedUsersUseCase {

    private final UserRepositoryPort userRepository;

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
    @Override
    public BlockedUserPageOutput execute(BlockedAccountPageInput input) {
        int currentPage = Math.max(1, input.getPage());
        int pageSize = normalizePageSize(input.getPageSize());
        Page<User> blockedUserPage = userRepository.findAllByBlockedUserTrueOrderByBlockedAtDescHandleAsc(PageRequest.of(currentPage - 1, pageSize));
        return new BlockedUserPageOutput(
                currentPage, pageSize, blockedUserPage.getTotalElements(), Math.max(1, blockedUserPage.getTotalPages()),
                blockedUserPage.getContent().stream()
                        .map(user -> new BlockedUserItemOutput(user.getHandle(), user.getLastAccessIp(), user.getBlockedAt()))
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
