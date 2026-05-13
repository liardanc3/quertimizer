package com.quertimizer.user.adapter.in.http.response;

import com.quertimizer.user.application.output.BlockedIpItemOutput;
import lombok.Data;

import java.time.LocalDateTime;

@Data
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
