package com.quertimizer.endpoint.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DuplicateCheckRes {

    private final boolean available;
    private final String reason;

    public static DuplicateCheckRes available() {
        return new DuplicateCheckRes(true, null);
    }

    public static DuplicateCheckRes duplicated(String reason) {
        return new DuplicateCheckRes(false, reason);
    }
}
