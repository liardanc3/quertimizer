package com.quertimizer.alarm.presentation.controller;

import com.quertimizer.alarm.presentation.dto.response.AlarmPageRes;
import com.quertimizer.alarm.application.service.AlarmService;
import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;
    private final AuthService authService;

    @GetMapping("/alarms")
    public ResponseEntity<AlarmPageRes> getAlarms(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(required = false) Integer pageSize,
                                                  Authentication authentication) {
        String currentHandle = resolveCurrentHandle(authentication);

        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(alarmService.getAlarms(currentHandle, page, pageSize));
    }

    @PostMapping("/alarms/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        String currentHandle = resolveCurrentHandle(authentication);

        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        alarmService.markAllRead(currentHandle);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/alarms/{alarmId}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long alarmId,
                                         Authentication authentication) {
        String currentHandle = resolveCurrentHandle(authentication);

        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return alarmService.markRead(alarmId, currentHandle)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private String resolveCurrentHandle(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authService.resolveCurrentHandle(authentication.getName());
    }

}
