package com.quertimizer.auth.application.port.in;

import com.quertimizer.auth.application.output.AuthBlockedIpOutput;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetAuthBlockedIpsUseCase {

    Page<AuthBlockedIpOutput> execute(Pageable pageable);
}
