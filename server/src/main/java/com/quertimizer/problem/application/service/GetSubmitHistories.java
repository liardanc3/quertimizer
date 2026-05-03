package com.quertimizer.problem.application.service;

import com.quertimizer.global.constant.ExecutionPlanElementIndexes;
import com.quertimizer.global.constant.MySqlExecutionPlanElementIndex;
import com.quertimizer.global.constant.PostgreSqlExecutionPlanElementIndex;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.input.SubmitHistorySearchInput;
import com.quertimizer.problem.application.output.SubmitHistoryListItemOutput;
import com.quertimizer.problem.application.output.SubmitHistoryPageOutput;
import com.quertimizer.problem.application.port.in.GetSubmitHistoriesUseCase;
import com.quertimizer.problem.application.port.out.ProblemSubmitHistoryRepositoryPort;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import com.quertimizer.problem.domain.model.SubmitHistoryPageConstant;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;
import org.springframework.data.domain.Sort;
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
     *   <li>제출 이력 필터링과 정렬
     *   <li>문제 번호 목록과 페이징 응답 생성
     * </ol>
     *
     * @param input 제출 이력 검색 조건
     */
    @Override
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

        List<ProblemSubmitHistory> histories = problemSubmitHistoryRepository.findAll(
                Sort.by(Sort.Direction.DESC, "submittedAt")
                        .and(Sort.by(Sort.Direction.DESC, "submitId"))
        );
        List<String> problemIds = histories.stream()
                .map(ProblemSubmitHistory::getProblemId)
                .distinct()
                .sorted(createProblemIdComparator())
                .toList();

        List<SubmitHistoryListItemOutput> filteredHistories = histories.stream()
                .filter(history -> matchesSubmitId(history, input.getSubmitId()))
                .filter(history -> matchesHandle(history, input.getQuery()))
                .filter(history -> matchesDbms(history, dbmsType))
                .filter(history -> matchesProblemId(history, input.getProblemId()))
                .filter(history -> matchesJudge(history, judgeFilter))
                .filter(history -> matchesPlanFilter(history, planFilterSelections))
                .sorted(createSubmitHistoryComparator(input.getCostSort()))
                .map(this::toSubmitHistoryListItemOutput)
                .toList();

        int totalCount = filteredHistories.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) SubmitHistoryPageConstant.PAGE_SIZE));
        int currentPage = Math.min(Math.max(input.getRequestedPage(), 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * SubmitHistoryPageConstant.PAGE_SIZE, totalCount);
        int toIndex = Math.min(fromIndex + SubmitHistoryPageConstant.PAGE_SIZE, totalCount);

        return new SubmitHistoryPageOutput(
                currentPage, SubmitHistoryPageConstant.PAGE_SIZE, totalCount, totalPages, problemIds,
                filteredHistories.subList(fromIndex, toIndex)
        );
    }

    private Comparator<ProblemSubmitHistory> createSubmitHistoryComparator(String costSort) {
        // 제출 기록 비교 기준 생성
        Comparator<ProblemSubmitHistory> defaultComparator = Comparator
                .comparing(ProblemSubmitHistory::getSubmittedAt, Comparator.reverseOrder())
                .thenComparing(ProblemSubmitHistory::getSubmitId, Comparator.reverseOrder());

        if ("asc".equalsIgnoreCase(costSort)) {
            return Comparator.comparingDouble(ProblemSubmitHistory::getCost)
                    .thenComparing(defaultComparator);
        }

        if ("desc".equalsIgnoreCase(costSort)) {
            return Comparator.comparingDouble(ProblemSubmitHistory::getCost)
                    .reversed()
                    .thenComparing(defaultComparator);
        }

        return defaultComparator;
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
        PlanFilterSelection legacySelection = createPlanFilterSelection(
                matchMode, scanBuckets, joinBuckets,
                filterBuckets, sortBuckets,
                aggregateBuckets, hintFilters
        );
        PlanFilterSelection postgresqlSelection = createPlanFilterSelection(
                matchMode, postgresqlScanBuckets, postgresqlJoinBuckets,
                postgresqlFilterBuckets, postgresqlSortBuckets,
                postgresqlAggregateBuckets, postgresqlHintFilters
        );
        PlanFilterSelection mysqlSelection = createPlanFilterSelection(
                matchMode, mysqlScanBuckets, mysqlJoinBuckets,
                mysqlFilterBuckets, mysqlSortBuckets,
                mysqlAggregateBuckets, mysqlHintFilters
        );

        return postgresqlSelection.hasFilters() || mysqlSelection.hasFilters()
                ? new PlanFilterSelectionsByDbms(postgresqlSelection, mysqlSelection)
                : new PlanFilterSelectionsByDbms(legacySelection, legacySelection);
    }

    private PlanFilterSelection createPlanFilterSelection(PlanMatchMode matchMode,
                                                          String scanBuckets, String joinBuckets,
                                                          String filterBuckets, String sortBuckets,
                                                          String aggregateBuckets, String hintFilters) {
        return new PlanFilterSelection(
                matchMode, parseFilterValues(scanBuckets), parseFilterValues(joinBuckets),
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

    private boolean matchesSubmitId(ProblemSubmitHistory history, String submitId) {
        // 제출 번호 일치 여부 확인
        if (submitId == null || submitId.isBlank()) {
            return true;
        }

        String normalizedSubmitId = submitId.trim();
        String formattedSubmitId = formatSubmitId(history.getSubmitId());
        String rawSubmitId = String.valueOf(history.getSubmitId() != null ? history.getSubmitId() : 0L);

        return formattedSubmitId.contains(normalizedSubmitId) || rawSubmitId.contains(normalizedSubmitId);
    }

    private String formatSubmitId(Long submitId) {
        // 제출 번호 포맷
        long resolvedSubmitId = submitId != null ? submitId : 0L;
        return String.format("%0" + SubmitHistoryPageConstant.SUBMIT_ID_LENGTH + "d", resolvedSubmitId);
    }

    private boolean matchesHandle(ProblemSubmitHistory history, String query) {
        // Handle 일치 여부 확인
        if (query == null || query.isBlank()) {
            return true;
        }

        return history.getHandle().toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT));
    }

    private boolean matchesDbms(ProblemSubmitHistory history, DbmsType dbmsType) {
        // DBMS 일치 여부 확인
        if (dbmsType == null) {
            return true;
        }

        return history.getDbmsType() == dbmsType;
    }

    private SubmitHistoryListItemOutput toSubmitHistoryListItemOutput(ProblemSubmitHistory history) {
        // 제출 기록 목록 항목 응답으로 변환
        DbmsType dbmsType = history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;
        long executionPlanElement = history.getExecutionPlanElement() != null ? history.getExecutionPlanElement() : 0L;

        return new SubmitHistoryListItemOutput(
                formatSubmitId(history.getSubmitId()),
                history.getHandle(),
                dbmsType.getValue(),
                history.getProblemId(),
                history.getSubmittedAt() != null ? history.getSubmittedAt().toString() : "",
                history.isSuccess(),
                history.getMessage() != null ? history.getMessage() : "",
                history.getSubmittedSql() != null ? history.getSubmittedSql() : "",
                history.getCost(),
                ExecutionPlanElementIndexes.normalize(dbmsType, executionPlanElement)
        );
    }

    private boolean matchesProblemId(ProblemSubmitHistory history, String problemId) {
        // 문제 번호 일치 여부 확인
        if (problemId == null || problemId.isBlank() || problemId.equalsIgnoreCase("all")) {
            return true;
        }

        return history.getProblemId() != null && history.getProblemId().contains(problemId.trim());
    }

    private boolean matchesJudge(ProblemSubmitHistory history, JudgeFilter judgeFilter) {
        // 채점 일치 여부 확인
        return switch (judgeFilter) {
            case ALL -> true;
            case SUCCESS -> history.isSuccess();
            case FAIL -> !history.isSuccess();
        };
    }

    private boolean matchesPlanFilter(ProblemSubmitHistory history, PlanFilterSelectionsByDbms selectionsByDbms) {
        // 실행 계획 필터 일치 여부 확인
        List<Boolean> matches = new ArrayList<>();
        DbmsType dbmsType = history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;
        PlanFilterSelection selection = selectionsByDbms.get(dbmsType);
        long executionPlanElement = ExecutionPlanElementIndexes.normalize(
                dbmsType,
                history.getExecutionPlanElement() != null ? history.getExecutionPlanElement() : 0L
        );

        selection.scanBuckets().forEach(value -> matches.add(matchesBucketFilter(dbmsType, executionPlanElement, "scanBucket", value)));
        selection.joinBuckets().forEach(value -> matches.add(matchesBucketFilter(dbmsType, executionPlanElement, "joinBucket", value)));
        selection.filterBuckets().forEach(value -> matches.add(matchesBucketFilter(dbmsType, executionPlanElement, "filterBucket", value)));
        selection.sortBuckets().forEach(value -> matches.add(matchesBucketFilter(dbmsType, executionPlanElement, "sortBucket", value)));
        selection.aggregateBuckets().forEach(value -> matches.add(matchesBucketFilter(dbmsType, executionPlanElement, "aggregateBucket", value)));
        selection.hintFilters().forEach(value -> matches.add(matchesHintFilter(executionPlanElement, value)));

        if (matches.isEmpty()) {
            return true;
        }

        return selection.matchMode() == PlanMatchMode.AND
                ? matches.stream().allMatch(Boolean::booleanValue)
                : matches.stream().anyMatch(Boolean::booleanValue);
    }

    private boolean matchesHintFilter(long executionPlanElement, String value) {
        // Hint 필터 일치 여부 확인
        if ("USED".equals(value)) {
            return hasPlanElement(executionPlanElement, ExecutionPlanElementIndexes.HINT_INDEX);
        }

        if ("UNUSED".equals(value)) {
            return !hasPlanElement(executionPlanElement, ExecutionPlanElementIndexes.HINT_INDEX);
        }

        return false;
    }

    private boolean matchesBucketFilter(DbmsType dbmsType, long executionPlanElement, String sectionKey, String value) {
        // 버킷 필터 일치 여부 확인
        if ("NONE".equals(value)) {
            return !hasAnyPlanElement(executionPlanElement, getSectionKnownIndexes(dbmsType, sectionKey));
        }

        int[] indexes = getBucketPlanIndexes(dbmsType, sectionKey, value);
        return indexes.length > 0 && hasAnyPlanElement(executionPlanElement, indexes);
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

    private boolean hasAnyPlanElement(long executionPlanElement, int[] indexes) {
        // Any 실행 계획 Element 여부 확인
        for (int index : indexes) {
            if (hasPlanElement(executionPlanElement, index)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasPlanElement(long executionPlanElement, int index) {
        // 실행 계획 Element 여부 확인
        return (executionPlanElement & (1L << index)) != 0;
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

        private PlanFilterSelection get(DbmsType dbmsType) {
            // get 조회
            return dbmsType == DbmsType.MYSQL ? mysql : postgresql;
        }
    }

}
