package com.quertimizer.endpoint.api.controller;

import com.quertimizer.endpoint.api.dto.response.RankPageRes;
import com.quertimizer.service.RankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    @GetMapping("/ranks")
    public ResponseEntity<RankPageRes> getRanks(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "postgresql") String dbms,
                                                @RequestParam(required = false) String query,
                                                @RequestParam(defaultValue = "solvedCount") String sortKey) {

        // 현재 DBMS, 검색, 정렬 기준 랭킹 페이지 반환
        return ResponseEntity.ok(rankService.getRanks(page, dbms, query, sortKey));
    }

}
