package com.quertimizer.ranking.presentation.controller;

import com.quertimizer.ranking.application.input.RankSearchInput;
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

    /**
     * 현재 DBMS, 검색어, 정렬 기준에 맞는 랭킹 페이지를 반환한다.
     *
     * <ol>
     *   <li>랭킹 검색 입력 생성
     *   <li>랭킹 페이지 응답 생성
     * </ol>
     *
     * @param page 요청 페이지 번호
     * @param pageSize 요청 페이지 크기
     * @param dbms 랭킹 기준 DBMS
     * @param query handle 검색어
     * @param sortKey 랭킹 정렬 기준
     */
    @GetMapping("/ranks")
    public ResponseEntity<RankPageRes> getRanks(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(required = false) Integer pageSize,
                                                @RequestParam(defaultValue = "postgresql") String dbms,
                                                @RequestParam(required = false) String query,
                                                @RequestParam(defaultValue = "solvedCount") String sortKey) {
        RankSearchInput input = new RankSearchInput(page, pageSize, dbms, query, sortKey);

        return ResponseEntity.ok(RankPageRes.from(getRanks.execute(input)));
    }

}
