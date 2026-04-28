package com.quertimizer.submit.presentation.controller;

import com.quertimizer.submit.application.input.SubmitHistorySearchInput;
import com.quertimizer.submit.application.usecase.GetSubmitHistories;
import com.quertimizer.submit.presentation.dto.response.SubmitHistoryPageRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SubmitHistoryController {

    private final GetSubmitHistories getSubmitHistories;

    /**
     * 제출 이력 검색 조건에 맞는 제출 이력 페이지를 반환한다.
     *
     * <ol>
     *   <li>제출 이력 검색 입력 생성
     *   <li>제출 이력 페이지 응답 생성
     * </ol>
     *
     * @param page 요청 페이지 번호
     * @param submitId 제출 번호 검색어
     * @param query handle 검색어
     * @param dbms DBMS 필터
     * @param problemId 문제 번호 필터
     * @param judge 채점 결과 필터
     * @param costSort cost 정렬 기준
     */
    @GetMapping("/submit-histories")
    public ResponseEntity<SubmitHistoryPageRes> getSubmitHistories(@RequestParam(defaultValue = "1") int page,
                                                                   @RequestParam(required = false) String submitId,
                                                                   @RequestParam(required = false) String query,
                                                                   @RequestParam(required = false) String dbms,
                                                                   @RequestParam(required = false) String problemId,
                                                                   @RequestParam(required = false) String judge,
                                                                   @RequestParam(defaultValue = "none") String costSort,
                                                                   @RequestParam(required = false) String planMatchMode,
                                                                   @RequestParam(required = false) String scanBuckets,
                                                                   @RequestParam(required = false) String joinBuckets,
                                                                   @RequestParam(required = false) String filterBuckets,
                                                                   @RequestParam(required = false) String sortBuckets,
                                                                   @RequestParam(required = false) String aggregateBuckets,
                                                                   @RequestParam(required = false) String hintFilters,
                                                                   @RequestParam(required = false) String postgresqlScanBuckets,
                                                                   @RequestParam(required = false) String postgresqlJoinBuckets,
                                                                   @RequestParam(required = false) String postgresqlFilterBuckets,
                                                                   @RequestParam(required = false) String postgresqlSortBuckets,
                                                                   @RequestParam(required = false) String postgresqlAggregateBuckets,
                                                                   @RequestParam(required = false) String postgresqlHintFilters,
                                                                   @RequestParam(required = false) String mysqlScanBuckets,
                                                                   @RequestParam(required = false) String mysqlJoinBuckets,
                                                                   @RequestParam(required = false) String mysqlFilterBuckets,
                                                                   @RequestParam(required = false) String mysqlSortBuckets,
                                                                   @RequestParam(required = false) String mysqlAggregateBuckets,
                                                                   @RequestParam(required = false) String mysqlHintFilters) {
        SubmitHistorySearchInput input = new SubmitHistorySearchInput(
                page,
                submitId,
                query,
                dbms,
                problemId,
                judge,
                costSort,
                planMatchMode,
                scanBuckets,
                joinBuckets,
                filterBuckets,
                sortBuckets,
                aggregateBuckets,
                hintFilters,
                postgresqlScanBuckets,
                postgresqlJoinBuckets,
                postgresqlFilterBuckets,
                postgresqlSortBuckets,
                postgresqlAggregateBuckets,
                postgresqlHintFilters,
                mysqlScanBuckets,
                mysqlJoinBuckets,
                mysqlFilterBuckets,
                mysqlSortBuckets,
                mysqlAggregateBuckets,
                mysqlHintFilters
        );

        return ResponseEntity.ok(SubmitHistoryPageRes.from(getSubmitHistories.execute(input)));
    }

}
