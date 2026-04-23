package com.quertimizer.auth.application.service;

import com.quertimizer.admin.presentation.dto.response.AdminBlockedIpItemRes;
import com.quertimizer.admin.presentation.dto.response.AdminBlockedIpPageRes;
import com.quertimizer.admin.presentation.dto.response.AdminBlockedUserItemRes;
import com.quertimizer.admin.presentation.dto.response.AdminBlockedUserPageRes;
import com.quertimizer.auth.domain.entity.BlockedIp;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.auth.infrastructure.repository.BlockedIpRepository;
import com.quertimizer.user.infrastructure.repository.UserRepository;
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
        return ipAddress != null && !ipAddress.isBlank() && blockedIpRepository.existsByIpAddress(ipAddress.trim());
    }

    public void blockUser(String handle) {
        User user = userRepository.findByHandle(handle)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));

        user.block();

        if (user.getLastAccessIp() == null || user.getLastAccessIp().isBlank()) {
            return;
        }

        blockedIpRepository.findById(user.getLastAccessIp().trim())
                .ifPresentOrElse(
                        blockedIp -> blockedIp.refresh(handle),
                        () -> blockedIpRepository.save(BlockedIp.create(user.getLastAccessIp().trim(), handle))
                );
    }

    public void unblockUser(String handle) {
        userRepository.findByHandle(handle).ifPresent(User::unblock);
        blockedIpRepository.deleteByBlockedHandle(handle);
    }

    public void unblockIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        blockedIpRepository.deleteById(ipAddress.trim());
    }

    @Transactional(readOnly = true)
    public AdminBlockedUserPageRes getBlockedUsers(int requestedPage, Integer requestedPageSize) {
        int currentPage = Math.max(1, requestedPage);
        int pageSize = normalizePageSize(requestedPageSize);
        Page<User> blockedUserPage = userRepository.findAllByBlockedUserTrueOrderByBlockedAtDescHandleAsc(PageRequest.of(currentPage - 1, pageSize));

        return new AdminBlockedUserPageRes(
                currentPage,
                pageSize,
                blockedUserPage.getTotalElements(),
                Math.max(1, blockedUserPage.getTotalPages()),
                blockedUserPage.getContent().stream()
                        .map(user -> new AdminBlockedUserItemRes(
                                user.getHandle(),
                                user.getLastAccessIp(),
                                user.getBlockedAt()
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public AdminBlockedIpPageRes getBlockedIps(int requestedPage, Integer requestedPageSize) {
        int currentPage = Math.max(1, requestedPage);
        int pageSize = normalizePageSize(requestedPageSize);
        Page<BlockedIp> blockedIpPage = blockedIpRepository.findAllByOrderByBlockedAtDescIpAddressAsc(PageRequest.of(currentPage - 1, pageSize));

        return new AdminBlockedIpPageRes(
                currentPage,
                pageSize,
                blockedIpPage.getTotalElements(),
                Math.max(1, blockedIpPage.getTotalPages()),
                blockedIpPage.getContent().stream()
                        .map(blockedIp -> new AdminBlockedIpItemRes(
                                blockedIp.getIpAddress(),
                                blockedIp.getBlockedAt()
                        ))
                        .toList()
        );
    }

    private int normalizePageSize(Integer requestedPageSize) {
        if (requestedPageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(MAX_PAGE_SIZE, Math.max(1, requestedPageSize));
    }

}
