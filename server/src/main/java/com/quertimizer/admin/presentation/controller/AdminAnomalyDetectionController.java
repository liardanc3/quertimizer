package com.quertimizer.admin.presentation.controller;

import com.quertimizer.admin.presentation.dto.response.AdminAnomalyTrendPageRes;
import com.quertimizer.admin.presentation.dto.response.AdminBlockedIpPageRes;
import com.quertimizer.admin.presentation.dto.response.AdminBlockedUserPageRes;
import com.quertimizer.auth.application.service.AccountRestrictionService;
import com.quertimizer.admin.application.service.AdminAnomalyDetectionService;
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
public class AdminAnomalyDetectionController {

    private final AdminAnomalyDetectionService adminAnomalyDetectionService;
    private final AccountRestrictionService accountRestrictionService;

    @GetMapping("/admin/anomaly-accounts/trends")
    public ResponseEntity<AdminAnomalyTrendPageRes> getAnomalyTrends(@RequestParam(defaultValue = "10m") String range,
                                                                     @RequestParam(required = false) String startedAt,
                                                                     @RequestParam(required = false) String endedAt,
                                                                     @RequestParam(defaultValue = "1") int page,
                                                                     @RequestParam(required = false) Integer pageSize) {

        return ResponseEntity.ok(adminAnomalyDetectionService.getSubmitTrend(range, startedAt, endedAt, page, pageSize));
    }

    @GetMapping("/admin/anomaly-accounts/blocked-users")
    public ResponseEntity<AdminBlockedUserPageRes> getBlockedUsers(@RequestParam(defaultValue = "1") int page,
                                                                   @RequestParam(required = false) Integer pageSize) {

        return ResponseEntity.ok(accountRestrictionService.getBlockedUsers(page, pageSize));
    }

    @GetMapping("/admin/anomaly-accounts/blocked-ips")
    public ResponseEntity<AdminBlockedIpPageRes> getBlockedIps(@RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(required = false) Integer pageSize) {

        return ResponseEntity.ok(accountRestrictionService.getBlockedIps(page, pageSize));
    }

    @PostMapping("/admin/anomaly-accounts/users/{handle}/block")
    public ResponseEntity<Void> blockUser(@PathVariable String handle) {

        accountRestrictionService.blockUser(handle);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/anomaly-accounts/users/{handle}/block")
    public ResponseEntity<Void> unblockUser(@PathVariable String handle) {

        accountRestrictionService.unblockUser(handle);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/anomaly-accounts/ips/{ipAddress}/block")
    public ResponseEntity<Void> unblockIp(@PathVariable String ipAddress) {

        accountRestrictionService.unblockIp(ipAddress);
        return ResponseEntity.noContent().build();
    }

}
