package com.quertimizer.auth.presentation.dto.response;

import com.quertimizer.auth.application.result.FoundHandleResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FindHandleRes {

    private final String handle;

    public static FindHandleRes from(FoundHandleResult result) {
        return new FindHandleRes(result.handle());
    }
}
