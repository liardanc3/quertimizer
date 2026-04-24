package com.quertimizer.user.presentation.controller;

import com.quertimizer.user.application.usecase.BlockUser;
import com.quertimizer.user.application.usecase.GetBlockedIps;
import com.quertimizer.user.application.usecase.GetBlockedUsers;
import com.quertimizer.user.application.usecase.GetUserAnomalyTrends;
import com.quertimizer.user.application.usecase.UnblockIp;
import com.quertimizer.user.application.usecase.UnblockUser;
import com.quertimizer.user.presentation.dto.response.BlockedIpPageRes;
import com.quertimizer.user.presentation.dto.response.BlockedUserPageRes;
import com.quertimizer.user.presentation.dto.response.UserAnomalyTrendPageRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserAnomalyController {

    private final GetUserAnomalyTrends getUserAnomalyTrends;
    private final GetBlockedUsers getBlockedUsers;
    private final GetBlockedIps getBlockedIps;
    private final BlockUser blockUser;
    private final UnblockUser unblockUser;
    private final UnblockIp unblockIp;

    @GetMapping("/admin/anomaly-accounts/trends")
    public ResponseEntity<UserAnomalyTrendPageRes> getAnomalyTrends(@RequestParam(defaultValue = "10m") String range,
                                                                    @RequestParam(required = false) String startedAt,
                                                                    @RequestParam(required = false) String endedAt,
                                                                    @RequestParam(defaultValue = "1") int page,
                                                                    @RequestParam(required = false) Integer pageSize) {
        // 이상 제출 추세를 조회
        return ResponseEntity.ok(UserAnomalyTrendPageRes.from(
                getUserAnomalyTrends.execute(range, startedAt, endedAt, page, pageSize)
        ));
    }

    @GetMapping("/admin/anomaly-accounts/blocked-users")
    public ResponseEntity<BlockedUserPageRes> getBlockedUsers(@RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(required = false) Integer pageSize) {
        // 차단된 사용자 목록을 조회
        return ResponseEntity.ok(BlockedUserPageRes.from(getBlockedUsers.execute(page, pageSize)));
    }

    @GetMapping("/admin/anomaly-accounts/blocked-ips")
    public ResponseEntity<BlockedIpPageRes> getBlockedIps(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(required = false) Integer pageSize) {
        // 차단된 IP 목록을 조회
        return ResponseEntity.ok(BlockedIpPageRes.from(getBlockedIps.execute(page, pageSize)));
    }

    @PostMapping("/admin/anomaly-accounts/users/{handle}/block")
    public ResponseEntity<Void> blockUser(@PathVariable String handle) {
        // 사용자를 차단
        blockUser.execute(handle);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/anomaly-accounts/users/{handle}/block")
    public ResponseEntity<Void> unblockUser(@PathVariable String handle) {
        // 사용자 차단을 해제
        unblockUser.execute(handle);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/anomaly-accounts/ips/{ipAddress}/block")
    public ResponseEntity<Void> unblockIp(@PathVariable String ipAddress) {
        // IP 차단을 해제
        unblockIp.execute(ipAddress);
        return ResponseEntity.noContent().build();
    }
}
