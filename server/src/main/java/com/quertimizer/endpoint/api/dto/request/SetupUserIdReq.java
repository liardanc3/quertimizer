package com.quertimizer.endpoint.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class SetupUserIdReq {

    @NotBlank(message = "\uC544\uC774\uB514\uB97C \uC785\uB825\uD574 \uC8FC\uC138\uC694.")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]{1,15}$",
            message = "\uC601\uBB38, \uC22B\uC790, \uC5B8\uB354\uC2A4\uCF54\uC5B4(_)\uC640 \uD558\uC774\uD508(-)\uB9CC \uC0AC\uC6A9\uD560 \uC218 \uC788\uC73C\uBA70 \uCD5C\uB300 15\uC790\uAE4C\uC9C0 \uC785\uB825\uD560 \uC218 \uC788\uC2B5\uB2C8\uB2E4."
    )
    private String userId;
}
