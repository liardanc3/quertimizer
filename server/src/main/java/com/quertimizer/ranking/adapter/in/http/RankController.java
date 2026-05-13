package com.quertimizer.ranking.adapter.in.http;

import com.quertimizer.ranking.application.port.in.GetRanksUseCase;
import com.quertimizer.ranking.adapter.in.http.request.RankSearchReq;
import com.quertimizer.ranking.adapter.in.http.response.RankPageRes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RankController {

    private final GetRanksUseCase getRanks;

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
    public ResponseEntity<RankPageRes> getRanks(@Valid RankSearchReq request) {
        return ResponseEntity.ok(RankPageRes.from(getRanks.execute(request.toInput())));
    }

}
