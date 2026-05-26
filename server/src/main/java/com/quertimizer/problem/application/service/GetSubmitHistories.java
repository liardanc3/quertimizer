package com.quertimizer.problem.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.input.SubmitHistorySearchInput;
import com.quertimizer.problem.application.output.SubmitHistoryListItemOutput;
import com.quertimizer.problem.application.output.SubmitHistoryPageOutput;
import com.quertimizer.problem.application.port.in.GetSubmitHistoriesUseCase;
import com.quertimizer.problem.application.port.out.ProblemSubmitHistoryRepositoryPort;
import com.quertimizer.problem.application.port.out.SubmitHistorySearchCondition;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import com.quertimizer.problem.domain.model.ExecutionPlanElementIndexes;
import com.quertimizer.problem.domain.model.MySqlExecutionPlanElementIndex;
import com.quertimizer.problem.domain.model.PostgreSqlExecutionPlanElementIndex;
import com.quertimizer.problem.domain.model.SubmitHistoryPageConstant;
import com.quertimizer.problem.domain.policy.SubmittedSqlFormatter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetSubmitHistories implements GetSubmitHistoriesUseCase {

    private final ProblemSubmitHistoryRepositoryPort problemSubmitHistoryRepository;

    /**
     * 제출 이력 검색 입력에 맞는 제출 이력 페이지를 생성한다.
     *
     * <ol>
     *   <li>DBMS, 채점 결과, 실행 계획 필터 확정
     *   <li>제출 이력 DB 검색과 정렬
     *   <li>문제 번호 목록과 페이징 응답 생성
     * </ol>
     *
     * @param input 제출 이력 검색 조건
     */
    @Override
    @Log("제출 목록 조회")
    public SubmitHistoryPageOutput execute(SubmitHistorySearchInput input) {
        DbmsType dbmsType = resolveDbmsType(input.getDbms());
        JudgeFilter judgeFilter = resolveJudgeFilter(input.getJudge());
        PlanFilterSelectionsByDbms planFilterSelections = resolvePlanFilterSelections(
                input.getPlanMatchMode(),
                input.getScanBuckets(),
                input.getJoinBuckets(),
                input.getFilterBuckets(),
                input.getSortBuckets(),
                input.getAggregateBuckets(),
                input.getHintFilters(),
                input.getPostgresqlScanBuckets(),
                input.getPostgresqlJoinBuckets(),
                input.getPostgresqlFilterBuckets(),
                input.getPostgresqlSortBuckets(),
                input.getPostgresqlAggregateBuckets(),
                input.getPostgresqlHintFilters(),
                input.getMysqlScanBuckets(),
                input.getMysqlJoinBuckets(),
                input.getMysqlFilterBuckets(),
                input.getMysqlSortBuckets(),
                input.getMysqlAggregateBuckets(),
                input.getMysqlHintFilters()
        );

        SubmitHistorySearchCondition searchCondition = createSearchCondition(
                input, dbmsType, judgeFilter, planFilterSelections
        );
        List<String> problemIds = problemSubmitHistoryRepository.findDistinctProblemIds().stream()
                .sorted(createProblemIdComparator())
                .toList();

        Page<ProblemSubmitHistory> histories = searchHistories(searchCondition, input.getCostSort(), input.getRequestedPage());
        int totalCount = (int) histories.getTotalElements();
        int totalPages = Math.max(1, histories.getTotalPages());
        int currentPage = Math.min(Math.max(input.getRequestedPage(), 1), totalPages);
        if (currentPage != histories.getNumber() + 1) {
            histories = searchHistories(searchCondition, input.getCostSort(), currentPage);
        }

        List<SubmitHistoryListItemOutput> historyOutputs = histories.getContent().stream()
                .map(this::toSubmitHistoryListItemOutput)
                .toList();

        return new SubmitHistoryPageOutput(
                currentPage, SubmitHistoryPageConstant.PAGE_SIZE, totalCount, totalPages, problemIds,
                historyOutputs
        );
    }

    private Page<ProblemSubmitHistory> searchHistories(SubmitHistorySearchCondition condition, String costSort, int requestedPage) {
        // 요청 페이지를 DB 페이징 조건으로 변환
        int currentPage = Math.max(requestedPage, 1);
        return problemSubmitHistoryRepository.search(
                condition,
                PageRequest.of(currentPage - 1, SubmitHistoryPageConstant.PAGE_SIZE),
                costSort
        );
    }

    private SubmitHistorySearchCondition createSearchCondition(SubmitHistorySearchInput input, DbmsType dbmsType,
                                                               JudgeFilter judgeFilter,
                                                               PlanFilterSelectionsByDbms planFilterSelections) {
        // 제출 이력 검색 조건 생성
        return new SubmitHistorySearchCondition(
                input.getSubmitId(), input.getQuery(), dbmsType, resolveProblemId(input.getProblemId()), resolveSuccess(judgeFilter),
                createPlanElementSearchCondition(planFilterSelections.postgresql()),
                createPlanElementSearchCondition(planFilterSelections.mysql())
        );
    }

    private String resolveProblemId(String problemId) {
        // 문제 번호 필터 결정
        if (problemId == null || problemId.isBlank() || problemId.equalsIgnoreCase("all")) {
            return null;
        }

        return problemId;
    }

    private Boolean resolveSuccess(JudgeFilter judgeFilter) {
        // 채점 결과 필터를 DB 검색 값으로 변환
        return switch (judgeFilter) {
            case ALL -> null;
            case SUCCESS -> true;
            case FAIL -> false;
        };
    }

    private SubmitHistorySearchCondition.PlanElementSearchCondition createPlanElementSearchCondition(PlanFilterSelection selection) {
        // 실행 계획 선택값을 비트 마스크 조건으로 변환
        List<SubmitHistorySearchCondition.PlanElementCondition> conditions = new ArrayList<>();
        DbmsType dbmsType = selection.dbmsType();
        selection.scanBuckets().forEach(value -> conditions.add(createBucketPlanCondition(dbmsType, "scanBucket", value)));
        selection.joinBuckets().forEach(value -> conditions.add(createBucketPlanCondition(dbmsType, "joinBucket", value)));
        selection.filterBuckets().forEach(value -> conditions.add(createBucketPlanCondition(dbmsType, "filterBucket", value)));
        selection.sortBuckets().forEach(value -> conditions.add(createBucketPlanCondition(dbmsType, "sortBucket", value)));
        selection.aggregateBuckets().forEach(value -> conditions.add(createBucketPlanCondition(dbmsType, "aggregateBucket", value)));
        selection.hintFilters().forEach(value -> conditions.add(createHintPlanCondition(value)));

        return conditions.isEmpty()
                ? SubmitHistorySearchCondition.PlanElementSearchCondition.empty()
                : new SubmitHistorySearchCondition.PlanElementSearchCondition(selection.matchMode() == PlanMatchMode.AND, conditions);
    }

    private SubmitHistorySearchCondition.PlanElementCondition createBucketPlanCondition(DbmsType dbmsType,
                                                                                       String sectionKey,
                                                                                       String value) {
        // 버킷 필터를 실행 계획 비트 마스크 조건으로 변환
        boolean matched = !"NONE".equals(value);
        int[] indexes = matched
                ? getBucketPlanIndexes(dbmsType, sectionKey, value)
                : getSectionKnownIndexes(dbmsType, sectionKey);
        return new SubmitHistorySearchCondition.PlanElementCondition(createPlanMask(indexes), matched);
    }

    private SubmitHistorySearchCondition.PlanElementCondition createHintPlanCondition(String value) {
        // Hint 필터를 실행 계획 비트 마스크 조건으로 변환
        if (!"USED".equals(value) && !"UNUSED".equals(value)) {
            return new SubmitHistorySearchCondition.PlanElementCondition(0L, true);
        }

        boolean matched = "USED".equals(value);
        long hintMask = createPlanMask(new int[]{ExecutionPlanElementIndexes.HINT_INDEX});
        return new SubmitHistorySearchCondition.PlanElementCondition(hintMask, matched);
    }

    private long createPlanMask(int[] indexes) {
        // 실행 계획 index 목록을 비트 마스크로 변환
        long mask = 0L;
        for (int index : indexes) {
            mask |= 1L << index;
        }

        return mask;
    }

    private DbmsType resolveDbmsType(String dbms) {
        // 요청 DBMS 값을 내부 유형으로 맞춤
        if (dbms == null || dbms.isBlank() || dbms.equalsIgnoreCase("all")) {
            return null;
        }

        return DbmsType.fromValue(dbms).orElse(DbmsType.POSTGRESQL);
    }

    private JudgeFilter resolveJudgeFilter(String judge) {
        // 채점 필터 결정
        if (judge == null || judge.isBlank() || judge.equalsIgnoreCase("all")) {
            return JudgeFilter.ALL;
        }

        return judge.equalsIgnoreCase("success") ? JudgeFilter.SUCCESS : JudgeFilter.FAIL;
    }

    private PlanMatchMode resolvePlanMatchMode(String planMatchMode) {
        // 실행 계획 매칭 모드 결정
        return planMatchMode != null && planMatchMode.equalsIgnoreCase("and") ? PlanMatchMode.AND : PlanMatchMode.OR;
    }

    private List<String> parseFilterValues(String rawValue) {
        // 필터 값 목록 파싱
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }

        return Arrays.stream(rawValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private PlanFilterSelectionsByDbms resolvePlanFilterSelections(String planMatchMode,
                                                                   String scanBuckets, String joinBuckets,
                                                                   String filterBuckets, String sortBuckets,
                                                                   String aggregateBuckets, String hintFilters,
                                                                   String postgresqlScanBuckets, String postgresqlJoinBuckets,
                                                                   String postgresqlFilterBuckets, String postgresqlSortBuckets,
                                                                   String postgresqlAggregateBuckets, String postgresqlHintFilters,
                                                                   String mysqlScanBuckets, String mysqlJoinBuckets,
                                                                   String mysqlFilterBuckets, String mysqlSortBuckets,
                                                                   String mysqlAggregateBuckets, String mysqlHintFilters) {
        PlanMatchMode matchMode = resolvePlanMatchMode(planMatchMode);
        PlanFilterSelection legacyPostgresqlSelection = createPlanFilterSelection(
                DbmsType.POSTGRESQL, matchMode, scanBuckets, joinBuckets,
                filterBuckets, sortBuckets,
                aggregateBuckets, hintFilters
        );
        PlanFilterSelection legacyMysqlSelection = createPlanFilterSelection(
                DbmsType.MYSQL, matchMode, scanBuckets, joinBuckets,
                filterBuckets, sortBuckets,
                aggregateBuckets, hintFilters
        );
        PlanFilterSelection postgresqlSelection = createPlanFilterSelection(
                DbmsType.POSTGRESQL, matchMode, postgresqlScanBuckets, postgresqlJoinBuckets,
                postgresqlFilterBuckets, postgresqlSortBuckets,
                postgresqlAggregateBuckets, postgresqlHintFilters
        );
        PlanFilterSelection mysqlSelection = createPlanFilterSelection(
                DbmsType.MYSQL, matchMode, mysqlScanBuckets, mysqlJoinBuckets,
                mysqlFilterBuckets, mysqlSortBuckets,
                mysqlAggregateBuckets, mysqlHintFilters
        );

        return postgresqlSelection.hasFilters() || mysqlSelection.hasFilters()
                ? new PlanFilterSelectionsByDbms(postgresqlSelection, mysqlSelection)
                : new PlanFilterSelectionsByDbms(legacyPostgresqlSelection, legacyMysqlSelection);
    }

    private PlanFilterSelection createPlanFilterSelection(DbmsType dbmsType, PlanMatchMode matchMode,
                                                          String scanBuckets, String joinBuckets,
                                                          String filterBuckets, String sortBuckets,
                                                          String aggregateBuckets, String hintFilters) {
        return new PlanFilterSelection(
                dbmsType, matchMode, parseFilterValues(scanBuckets), parseFilterValues(joinBuckets),
                parseFilterValues(filterBuckets), parseFilterValues(sortBuckets),
                parseFilterValues(aggregateBuckets), parseFilterValues(hintFilters)
        );
    }

    private Comparator<String> createProblemIdComparator() {
        // 문제 번호 비교 기준 생성
        return Comparator.comparingInt(this::toProblemNumber).thenComparing(String::compareTo);
    }

    private int toProblemNumber(String problemId) {
        // 문제 번호 변환
        String[] tokens = problemId.split("-");
        try {
            return Integer.parseInt(tokens.length > 0 ? tokens[0] : problemId);
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }

    private String formatSubmitId(Long submitId) {
        // 제출 번호 포맷
        long resolvedSubmitId = submitId != null ? submitId : 0L;
        return String.format("%0" + SubmitHistoryPageConstant.SUBMIT_ID_LENGTH + "d", resolvedSubmitId);
    }

    private SubmitHistoryListItemOutput toSubmitHistoryListItemOutput(ProblemSubmitHistory history) {
        // 제출 기록 목록 항목 응답으로 변환
        DbmsType dbmsType = history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;
        long executionPlanElement = history.isSuccess() && history.getExecutionPlanElement() != null
                ? history.getExecutionPlanElement()
                : 0L;
        double cost = history.isSuccess() ? history.getCost() : 0d;

        return new SubmitHistoryListItemOutput(
                formatSubmitId(history.getSubmitId()),
                history.getHandle(),
                dbmsType.getValue(),
                history.getProblemId(),
                history.getSubmittedAt() != null ? history.getSubmittedAt().toString() : "",
                history.isSuccess(),
                history.getMessage() != null ? history.getMessage() : "",
                SubmittedSqlFormatter.format(history.getSubmittedSql()),
                cost,
                ExecutionPlanElementIndexes.normalize(dbmsType, executionPlanElement)
        );
    }

    private int[] getSectionKnownIndexes(DbmsType dbmsType, String sectionKey) {
        // Section Known Indexes 조회
        Set<Integer> knownIndexes = new LinkedHashSet<>();
        for (String value : getSectionSupportedValues(dbmsType, sectionKey)) {
            if ("NONE".equals(value) || "OTHERS".equals(value)) {
                continue;
            }

            for (int index : getBucketPlanIndexes(dbmsType, sectionKey, value)) {
                knownIndexes.add(index);
            }
        }

        return knownIndexes.stream().mapToInt(Integer::intValue).toArray();
    }

    private List<String> getSectionSupportedValues(DbmsType dbmsType, String sectionKey) {
        // Section Supported 값 목록 조회
        return switch (dbmsType) {
            case MYSQL -> switch (sectionKey) {
                case "scanBucket" -> List.of("FULL_TABLE_SCAN", "INDEX_SCAN", "RANGE_SCAN", "REF_SCAN", "CONST_SCAN", "DERIVED_SCAN", "OTHERS");
                case "joinBucket" -> List.of("NONE", "NESTED_LOOP", "HASH_JOIN", "JOIN_BUFFER", "OTHERS");
                case "filterBucket" -> List.of("NONE", "INDEX_CONDITION", "ATTACHED_CONDITION", "FILTER_CONDITION", "OTHERS");
                case "sortBucket" -> List.of("NONE", "FILESORT", "TEMPORARY_TABLE", "OTHERS");
                case "aggregateBucket" -> List.of("NONE", "GROUPING_OPERATION", "WINDOW_OPERATION", "AGGREGATE", "OTHERS");
                default -> List.of();
            };
            case POSTGRESQL -> switch (sectionKey) {
                case "scanBucket" -> List.of("FULL_SCAN", "INDEX_SCAN", "BITMAP_SCAN", "TID_SCAN", "DERIVED_SCAN", "OTHERS");
                case "joinBucket" -> List.of("NONE", "NESTED_LOOP", "MERGE_JOIN", "HASH_JOIN", "OTHERS");
                case "filterBucket" -> List.of("NONE", "ACCESS_FILTER", "POST_FILTER", "JOIN_FILTER", "OTHERS");
                case "sortBucket" -> List.of("NONE", "PLAIN_SORT", "INCREMENTAL_SORT", "OTHERS");
                case "aggregateBucket" -> List.of(
                        "NONE", "PLAIN_AGG", "GROUP_AGG", "HASH_AGG", "MIXED_AGG",
                        "WINDOW_AGG", "UNIQUE_AGG", "SET_AGG", "OTHERS"
                );
                default -> List.of();
            };
        };
    }

    private int[] getBucketPlanIndexes(DbmsType dbmsType, String sectionKey, String value) {
        // 버킷 실행 계획 Indexes 조회
        return switch (dbmsType) {
            case MYSQL -> getMySqlBucketPlanIndexes(sectionKey, value);
            case POSTGRESQL -> getPostgreSqlBucketPlanIndexes(sectionKey, value);
        };
    }

    private int[] getPostgreSqlBucketPlanIndexes(String sectionKey, String value) {
        // Postgre SQL 버킷 실행 계획 Indexes 조회
        return switch (sectionKey) {
            case "scanBucket" -> switch (value) {
                case "FULL_SCAN" -> new int[]{PostgreSqlExecutionPlanElementIndex.FULL_SCAN, PostgreSqlExecutionPlanElementIndex.SEQ_SCAN};
                case "INDEX_SCAN" -> new int[]{PostgreSqlExecutionPlanElementIndex.INDEX_SCAN, PostgreSqlExecutionPlanElementIndex.INDEX_ONLY_SCAN};
                case "BITMAP_SCAN" -> new int[]{
                        PostgreSqlExecutionPlanElementIndex.BITMAP_INDEX_SCAN,
                        PostgreSqlExecutionPlanElementIndex.BITMAP_HEAP_SCAN
                };
                case "TID_SCAN" -> new int[]{PostgreSqlExecutionPlanElementIndex.TID_SCAN};
                case "DERIVED_SCAN" -> new int[]{
                        PostgreSqlExecutionPlanElementIndex.SUBQUERY_SCAN,
                        PostgreSqlExecutionPlanElementIndex.CTE_SCAN,
                        PostgreSqlExecutionPlanElementIndex.FUNCTION_SCAN,
                        PostgreSqlExecutionPlanElementIndex.VALUES_SCAN
                };
                default -> new int[0];
            };
            case "joinBucket" -> switch (value) {
                case "NESTED_LOOP" -> new int[]{PostgreSqlExecutionPlanElementIndex.NESTED_LOOP};
                case "MERGE_JOIN" -> new int[]{PostgreSqlExecutionPlanElementIndex.MERGE_JOIN};
                case "HASH_JOIN" -> new int[]{PostgreSqlExecutionPlanElementIndex.HASH_JOIN};
                default -> new int[0];
            };
            case "filterBucket" -> switch (value) {
                case "ACCESS_FILTER" -> new int[]{PostgreSqlExecutionPlanElementIndex.INDEX_CONDITION};
                case "POST_FILTER" -> new int[]{PostgreSqlExecutionPlanElementIndex.FILTER};
                default -> new int[0];
            };
            case "sortBucket" -> switch (value) {
                case "PLAIN_SORT" -> new int[]{PostgreSqlExecutionPlanElementIndex.SORT};
                case "INCREMENTAL_SORT" -> new int[]{PostgreSqlExecutionPlanElementIndex.INCREMENTAL_SORT};
                default -> new int[0];
            };
            case "aggregateBucket" -> switch (value) {
                case "GROUP_AGG" -> new int[]{PostgreSqlExecutionPlanElementIndex.GROUP_AGGREGATE};
                case "HASH_AGG" -> new int[]{PostgreSqlExecutionPlanElementIndex.HASH_AGGREGATE};
                case "UNIQUE_AGG" -> new int[]{PostgreSqlExecutionPlanElementIndex.UNIQUE};
                default -> new int[0];
            };
            default -> new int[0];
        };
    }

    private int[] getMySqlBucketPlanIndexes(String sectionKey, String value) {
        // MySQL 버킷 실행 계획 Indexes 조회
        return switch (sectionKey) {
            case "scanBucket" -> switch (value) {
                case "FULL_TABLE_SCAN" -> new int[]{MySqlExecutionPlanElementIndex.FULL_TABLE_SCAN};
                case "INDEX_SCAN" -> new int[]{MySqlExecutionPlanElementIndex.INDEX_SCAN};
                case "RANGE_SCAN" -> new int[]{MySqlExecutionPlanElementIndex.RANGE_SCAN};
                case "REF_SCAN" -> new int[]{MySqlExecutionPlanElementIndex.REF_SCAN, MySqlExecutionPlanElementIndex.EQ_REF_SCAN};
                case "CONST_SCAN" -> new int[]{MySqlExecutionPlanElementIndex.CONST_SCAN};
                case "DERIVED_SCAN" -> new int[]{MySqlExecutionPlanElementIndex.DERIVED_TABLE, MySqlExecutionPlanElementIndex.MATERIALIZED_SUBQUERY};
                default -> new int[0];
            };
            case "joinBucket" -> switch (value) {
                case "NESTED_LOOP" -> new int[]{MySqlExecutionPlanElementIndex.NESTED_LOOP_JOIN};
                case "HASH_JOIN" -> new int[]{MySqlExecutionPlanElementIndex.HASH_JOIN};
                case "JOIN_BUFFER" -> new int[]{MySqlExecutionPlanElementIndex.USING_JOIN_BUFFER};
                default -> new int[0];
            };
            case "filterBucket" -> switch (value) {
                case "INDEX_CONDITION" -> new int[]{MySqlExecutionPlanElementIndex.INDEX_CONDITION};
                case "ATTACHED_CONDITION" -> new int[]{MySqlExecutionPlanElementIndex.ATTACHED_CONDITION};
                case "FILTER_CONDITION" -> new int[]{MySqlExecutionPlanElementIndex.FILTER_CONDITION};
                default -> new int[0];
            };
            case "sortBucket" -> switch (value) {
                case "FILESORT" -> new int[]{MySqlExecutionPlanElementIndex.FILESORT};
                case "TEMPORARY_TABLE" -> new int[]{MySqlExecutionPlanElementIndex.TEMPORARY_TABLE};
                default -> new int[0];
            };
            case "aggregateBucket" -> switch (value) {
                case "GROUPING_OPERATION" -> new int[]{MySqlExecutionPlanElementIndex.GROUPING_OPERATION};
                case "WINDOW_OPERATION" -> new int[]{MySqlExecutionPlanElementIndex.WINDOW_OPERATION};
                case "AGGREGATE" -> new int[]{MySqlExecutionPlanElementIndex.AGGREGATE};
                default -> new int[0];
            };
            default -> new int[0];
        };
    }

    private enum JudgeFilter {
        ALL,
        SUCCESS,
        FAIL
    }

    private enum PlanMatchMode {
        AND,
        OR
    }

    @Value
    @Accessors(fluent = true)
    private static class PlanFilterSelection {
        DbmsType dbmsType;
        PlanMatchMode matchMode;
        List<String> scanBuckets;
        List<String> joinBuckets;
        List<String> filterBuckets;
        List<String> sortBuckets;
        List<String> aggregateBuckets;
        List<String> hintFilters;

        private boolean hasFilters() {
            // 필터 목록 여부 확인
            return !scanBuckets.isEmpty()
                    || !joinBuckets.isEmpty()
                    || !filterBuckets.isEmpty()
                    || !sortBuckets.isEmpty()
                    || !aggregateBuckets.isEmpty()
                    || !hintFilters.isEmpty();
        }
    }

    @Value
    @Accessors(fluent = true)
    private static class PlanFilterSelectionsByDbms {
        PlanFilterSelection postgresql;
        PlanFilterSelection mysql;

    }

}
