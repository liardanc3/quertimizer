package com.quertimizer.user.presentation.dto.response;

import com.quertimizer.auth.application.output.BlockedIpItemOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BlockedIpItemRes {

    private final String ipAddress;
    private final LocalDateTime blockedAt;

    public static BlockedIpItemRes from(BlockedIpItemOutput result) {
        return new BlockedIpItemRes(
                result.getIpAddress(),
                result.getBlockedAt()
        );
    }
}
