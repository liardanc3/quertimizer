package com.quertimizer.user.adapter.in.web.response;

import com.quertimizer.user.application.output.BlockedUserItemOutput;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlockedUserItemRes {

    private final String handle;
    private final String ipAddress;
    private final LocalDateTime blockedAt;

    public static BlockedUserItemRes from(BlockedUserItemOutput result) {
        return new BlockedUserItemRes(
                result.getHandle(),
                result.getIpAddress(),
                result.getBlockedAt()
        );
    }
}
