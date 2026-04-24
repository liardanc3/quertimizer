package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.output.BlockedIpItemOutput;
import com.quertimizer.auth.application.output.BlockedIpPageOutput;
import com.quertimizer.auth.application.output.BlockedUserItemOutput;
import com.quertimizer.auth.application.output.BlockedUserPageOutput;
import com.quertimizer.auth.domain.entity.BlockedIp;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.auth.application.port.BlockedIpRepository;
import com.quertimizer.user.application.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.quertimizer.auth.domain.model.AuthFailReason.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountRestrictionService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final BlockedIpRepository blockedIpRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean isBlockedIp(String ipAddress) {
        // 공백이 아닌 IP만 차단 목록에서 확인한다.
        return ipAddress != null && !ipAddress.isBlank() && blockedIpRepository.existsByIpAddress(ipAddress.trim());
    }

    public void blockUser(String handle) {
        // Handle로 차단 대상을 조회한다.
        User user = userRepository.findByHandle(handle)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));

        // 사용자 차단 상태를 반영한다.
        user.block();

        if (user.getLastAccessIp() == null || user.getLastAccessIp().isBlank()) {
            return;
        }

        // 마지막 접속 IP도 함께 차단 목록에 반영한다.
        blockedIpRepository.findById(user.getLastAccessIp().trim())
                .ifPresentOrElse(
                        blockedIp -> blockedIp.refresh(handle),
                        () -> blockedIpRepository.save(BlockedIp.create(user.getLastAccessIp().trim(), handle))
                );
    }

    public void unblockUser(String handle) {
        // 사용자 차단과 연결된 IP 차단을 함께 해제한다.
        userRepository.findByHandle(handle).ifPresent(User::unblock);
        blockedIpRepository.deleteByBlockedHandle(handle);
    }

    public void unblockIp(String ipAddress) {
        // 차단된 IP를 해제
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        blockedIpRepository.deleteById(ipAddress.trim());
    }

    @Transactional(readOnly = true)
    public BlockedUserPageOutput getBlockedUsers(int requestedPage, Integer requestedPageSize) {
        // 차단된 사용자 목록을 조회
        int currentPage = Math.max(1, requestedPage);
        int pageSize = normalizePageSize(requestedPageSize);
        Page<User> blockedUserPage = userRepository.findAllByBlockedUserTrueOrderByBlockedAtDescHandleAsc(PageRequest.of(currentPage - 1, pageSize));

        return new BlockedUserPageOutput(
                currentPage,
                pageSize,
                blockedUserPage.getTotalElements(),
                Math.max(1, blockedUserPage.getTotalPages()),
                blockedUserPage.getContent().stream()
                        .map(user -> new BlockedUserItemOutput(
                                user.getHandle(),
                                user.getLastAccessIp(),
                                user.getBlockedAt()
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public BlockedIpPageOutput getBlockedIps(int requestedPage, Integer requestedPageSize) {
        // 차단된 IP 목록을 조회
        int currentPage = Math.max(1, requestedPage);
        int pageSize = normalizePageSize(requestedPageSize);
        Page<BlockedIp> blockedIpPage = blockedIpRepository.findAllByOrderByBlockedAtDescIpAddressAsc(PageRequest.of(currentPage - 1, pageSize));

        return new BlockedIpPageOutput(
                currentPage,
                pageSize,
                blockedIpPage.getTotalElements(),
                Math.max(1, blockedIpPage.getTotalPages()),
                blockedIpPage.getContent().stream()
                        .map(blockedIp -> new BlockedIpItemOutput(
                                blockedIp.getIpAddress(),
                                blockedIp.getBlockedAt()
                        ))
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
