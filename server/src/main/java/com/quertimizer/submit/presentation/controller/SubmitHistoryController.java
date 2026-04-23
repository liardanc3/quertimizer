package com.quertimizer.submit.presentation.controller;

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
                                                                   @RequestParam(required = false) String oracleScanBuckets,
                                                                   @RequestParam(required = false) String oracleJoinBuckets,
                                                                   @RequestParam(required = false) String oracleFilterBuckets,
                                                                   @RequestParam(required = false) String oracleSortBuckets,
                                                                   @RequestParam(required = false) String oracleAggregateBuckets,
                                                                   @RequestParam(required = false) String oracleHintFilters) {

        return ResponseEntity.ok(SubmitHistoryPageRes.from(getSubmitHistories.execute(
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
                oracleScanBuckets,
                oracleJoinBuckets,
                oracleFilterBuckets,
                oracleSortBuckets,
                oracleAggregateBuckets,
                oracleHintFilters
        )));
    }

}
