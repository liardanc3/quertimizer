package com.quertimizer.service;

import com.quertimizer.endpoint.api.dto.response.AdminBlockedIpItemRes;
import com.quertimizer.endpoint.api.dto.response.AdminBlockedIpPageRes;
import com.quertimizer.endpoint.api.dto.response.AdminBlockedUserItemRes;
import com.quertimizer.endpoint.api.dto.response.AdminBlockedUserPageRes;
import com.quertimizer.entity.BlockedIp;
import com.quertimizer.entity.BlockedUser;
import com.quertimizer.entity.User;
import com.quertimizer.exception.BusinessException;
import com.quertimizer.repository.BlockedIpRepository;
import com.quertimizer.repository.BlockedUserRepository;
import com.quertimizer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountRestrictionService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final String USER_NOT_FOUND_MESSAGE = "존재하지 않는 사용자입니다.";

    private final BlockedUserRepository blockedUserRepository;
    private final BlockedIpRepository blockedIpRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean isBlockedUser(String userId) {
        return userId != null && !userId.isBlank() && blockedUserRepository.existsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean isBlockedIp(String ipAddress) {
        return ipAddress != null && !ipAddress.isBlank() && blockedIpRepository.existsByIpAddress(ipAddress.trim());
    }

    public void blockUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));

        blockedUserRepository.findById(userId)
                .ifPresentOrElse(
                        blockedUser -> blockedUser.refresh(user.getLastAccessIp()),
                        () -> blockedUserRepository.save(BlockedUser.create(userId, user.getLastAccessIp()))
                );

        if (user.getLastAccessIp() == null || user.getLastAccessIp().isBlank()) {
            return;
        }

        blockedIpRepository.findById(user.getLastAccessIp().trim())
                .ifPresentOrElse(
                        blockedIp -> blockedIp.refresh(userId),
                        () -> blockedIpRepository.save(BlockedIp.create(user.getLastAccessIp().trim(), userId))
                );
    }

    public void unblockUser(String userId) {
        blockedUserRepository.deleteById(userId);
        blockedIpRepository.deleteByBlockedUserId(userId);
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
        Page<BlockedUser> blockedUserPage = blockedUserRepository.findAllByOrderByBlockedAtDescUserIdAsc(PageRequest.of(currentPage - 1, pageSize));

        return new AdminBlockedUserPageRes(
                currentPage,
                pageSize,
                blockedUserPage.getTotalElements(),
                Math.max(1, blockedUserPage.getTotalPages()),
                blockedUserPage.getContent().stream()
                        .map(blockedUser -> new AdminBlockedUserItemRes(
                                blockedUser.getUserId(),
                                blockedUser.getLastAccessIp(),
                                blockedUser.getBlockedAt()
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
