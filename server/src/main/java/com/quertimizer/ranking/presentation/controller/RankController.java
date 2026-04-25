package com.quertimizer.ranking.presentation.controller;

import com.quertimizer.ranking.application.usecase.GetRanks;
import com.quertimizer.ranking.presentation.dto.response.RankPageRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RankController {

    private final GetRanks getRanks;

    @GetMapping("/ranks")
    public ResponseEntity<RankPageRes> getRanks(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(required = false) Integer pageSize,
                                                @RequestParam(defaultValue = "postgresql") String dbms,
                                                @RequestParam(required = false) String query,
                                                @RequestParam(defaultValue = "solvedCount") String sortKey) {

        // 현재 DBMS, 검색, 정렬 기준 랭킹 페이지 반환
        return ResponseEntity.ok(RankPageRes.from(getRanks.execute(page, pageSize, dbms, query, sortKey)));
    }

}
