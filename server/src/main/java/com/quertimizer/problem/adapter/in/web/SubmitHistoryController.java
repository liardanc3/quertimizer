package com.quertimizer.problem.adapter.in.web;

import com.quertimizer.problem.application.port.in.GetSubmitHistoriesUseCase;
import com.quertimizer.problem.adapter.in.web.request.SubmitHistorySearchRequest;
import com.quertimizer.problem.adapter.in.web.response.SubmitHistoryPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SubmitHistoryController {

    private final GetSubmitHistoriesUseCase getSubmitHistoriesUseCase;

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
    public ResponseEntity<SubmitHistoryPageResponse> getSubmitHistories(@Valid SubmitHistorySearchRequest request) {
        return ResponseEntity.ok(SubmitHistoryPageResponse.from(getSubmitHistoriesUseCase.execute(request.toInput())));
    }

}
