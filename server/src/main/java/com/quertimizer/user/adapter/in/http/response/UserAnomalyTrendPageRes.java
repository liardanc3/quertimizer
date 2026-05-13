package com.quertimizer.user.adapter.in.http.response;

import com.quertimizer.user.application.output.UserAnomalyTrendPageOutput;
import lombok.Data;

import java.util.List;

@Data
public class UserAnomalyTrendPageRes {

    private final int currentPage;
    private final int pageSize;
    private final long totalCount;
    private final int totalPages;
    private final List<UserAnomalyTrendItemRes> items;

    public static UserAnomalyTrendPageRes from(UserAnomalyTrendPageOutput result) {
        return new UserAnomalyTrendPageRes(
                result.getCurrentPage(),
                result.getPageSize(),
                result.getTotalCount(),
                result.getTotalPages(),
                result.getItems().stream()
                        .map(UserAnomalyTrendItemRes::from)
                        .toList()
        );
    }
}
