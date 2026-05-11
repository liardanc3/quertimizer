package com.quertimizer.user.application.port.out;

import com.quertimizer.user.domain.model.UserBlockedIp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserAccountRestrictionPort {

    void blockIp(String ipAddress, String handle);

    void unblockHandle(String handle);

    void unblockIp(String ipAddress);

    Page<UserBlockedIp> findBlockedIps(Pageable pageable);
}
