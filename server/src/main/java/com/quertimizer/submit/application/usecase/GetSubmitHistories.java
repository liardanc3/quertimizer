package com.quertimizer.submit.application.usecase;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.global.constant.ExecutionPlanElementIndexes;
import com.quertimizer.global.constant.OracleExecutionPlanElementIndex;
import com.quertimizer.global.constant.PostgreSqlExecutionPlanElementIndex;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import com.quertimizer.problem.application.port.ProblemSubmitHistoryRepository;
import com.quertimizer.submit.application.output.SubmitHistoryListItemOutput;
import com.quertimizer.submit.application.output.SubmitHistoryPageOutput;
import lombok.RequiredArgsConstructor;
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
public class GetSubmitHistories {

    private static final int SUBMIT_HISTORY_PAGE_SIZE = 10;
    private static final int HINT_INDEX = 30;
    private static final int SUBMIT_ID_LENGTH = 8;

    private final ProblemSubmitHistoryRepository problemSubmitHistoryRepository;

    public SubmitHistoryPageOutput execute(int requestedPage,
                                           String submitId,
                                           String query,
                                           String dbms,
                                           String problemId,
                                           String judge,
                                           String costSort,
                                           String planMatchMode,
                                           String scanBuckets,
                                           String joinBuckets,
                                           String filterBuckets,
                                           String sortBuckets,
                                           String aggregateBuckets,
                                           String hintFilters,
                                           String postgresqlScanBuckets,
                                           String postgresqlJoinBuckets,
                                           String postgresqlFilterBuckets,
                                           String postgresqlSortBuckets,
                                           String postgresqlAggregateBuckets,
                                           String postgresqlHintFilters,
                                           String oracleScanBuckets,
                                           String oracleJoinBuckets,
                                           String oracleFilterBuckets,
                                           String oracleSortBuckets,
                                           String oracleAggregateBuckets,
                                           String oracleHintFilters) {
        DbmsType dbmsType = resolveDbmsType(dbms);
        JudgeFilter judgeFilter = resolveJudgeFilter(judge);
        PlanFilterSelectionsByDbms planFilterSelections = resolvePlanFilterSelections(
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
        );

        List<ProblemSubmitHistory> histories = problemSubmitHistoryRepository.findAll(
                Sort.by(Sort.Direction.DESC, "submittedAt").and(Sort.by(Sort.Direction.DESC, "submitId"))
        );
        List<String> problemIds = histories.stream()
                .map(ProblemSubmitHistory::getProblemId)
                .distinct()
                .sorted(createProblemIdComparator())
                .toList();

        List<SubmitHistoryListItemOutput> filteredHistories = histories.stream()
                .filter(history -> matchesSubmitId(history, submitId))
                .filter(history -> matchesHandle(history, query))
                .filter(history -> matchesDbms(history, dbmsType))
                .filter(history -> matchesProblemId(history, problemId))
                .filter(history -> matchesJudge(history, judgeFilter))
                .filter(history -> matchesPlanFilter(history, planFilterSelections))
                .sorted(createSubmitHistoryComparator(costSort))
                .map(this::toSubmitHistoryListItemOutput)
                .toList();

        int totalCount = filteredHistories.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) SUBMIT_HISTORY_PAGE_SIZE));
        int currentPage = Math.min(Math.max(requestedPage, 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * SUBMIT_HISTORY_PAGE_SIZE, totalCount);
        int toIndex = Math.min(fromIndex + SUBMIT_HISTORY_PAGE_SIZE, totalCount);

        return new SubmitHistoryPageOutput(
                currentPage,
                SUBMIT_HISTORY_PAGE_SIZE,
                totalCount,
                totalPages,
                problemIds,
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
        // DBMS 유형 결정
        if (dbms == null || dbms.isBlank() || dbms.equalsIgnoreCase("all")) {
            return null;
        }

        return dbms.equalsIgnoreCase(DbmsType.ORACLE.getValue()) ? DbmsType.ORACLE : DbmsType.POSTGRESQL;
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
                                                                   String scanBuckets,
                                                                   String joinBuckets,
                                                                   String filterBuckets,
                                                                   String sortBuckets,
                                                                   String aggregateBuckets,
                                                                   String hintFilters,
                                                                   String postgresqlScanBuckets,
                                                                   String postgresqlJoinBuckets,
                                                                   String postgresqlFilterBuckets,
                                                                   String postgresqlSortBuckets,
                                                                   String postgresqlAggregateBuckets,
                                                                   String postgresqlHintFilters,
                                                                   String oracleScanBuckets,
                                                                   String oracleJoinBuckets,
                                                                   String oracleFilterBuckets,
                                                                   String oracleSortBuckets,
                                                                   String oracleAggregateBuckets,
                                                                   String oracleHintFilters) {
        PlanMatchMode matchMode = resolvePlanMatchMode(planMatchMode);
        PlanFilterSelection legacySelection = createPlanFilterSelection(
                matchMode,
                scanBuckets,
                joinBuckets,
                filterBuckets,
                sortBuckets,
                aggregateBuckets,
                hintFilters
        );
        PlanFilterSelection postgresqlSelection = createPlanFilterSelection(
                matchMode,
                postgresqlScanBuckets,
                postgresqlJoinBuckets,
                postgresqlFilterBuckets,
                postgresqlSortBuckets,
                postgresqlAggregateBuckets,
                postgresqlHintFilters
        );
        PlanFilterSelection oracleSelection = createPlanFilterSelection(
                matchMode,
                oracleScanBuckets,
                oracleJoinBuckets,
                oracleFilterBuckets,
                oracleSortBuckets,
                oracleAggregateBuckets,
                oracleHintFilters
        );

        return postgresqlSelection.hasFilters() || oracleSelection.hasFilters()
                ? new PlanFilterSelectionsByDbms(postgresqlSelection, oracleSelection)
                : new PlanFilterSelectionsByDbms(legacySelection, legacySelection);
    }

    private PlanFilterSelection createPlanFilterSelection(PlanMatchMode matchMode,
                                                          String scanBuckets,
                                                          String joinBuckets,
                                                          String filterBuckets,
                                                          String sortBuckets,
                                                          String aggregateBuckets,
                                                          String hintFilters) {
        return new PlanFilterSelection(
                matchMode,
                parseFilterValues(scanBuckets),
                parseFilterValues(joinBuckets),
                parseFilterValues(filterBuckets),
                parseFilterValues(sortBuckets),
                parseFilterValues(aggregateBuckets),
                parseFilterValues(hintFilters)
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
        return String.format("%0" + SUBMIT_ID_LENGTH + "d", resolvedSubmitId);
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
            return hasPlanElement(executionPlanElement, HINT_INDEX);
        }

        if ("UNUSED".equals(value)) {
            return !hasPlanElement(executionPlanElement, HINT_INDEX);
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
            case ORACLE -> switch (sectionKey) {
                case "scanBucket" -> List.of("FULL_SCAN", "ROWID_ACCESS", "INDEX_SCAN", "BITMAP_SCAN", "DERIVED_SCAN", "REMOTE_SCAN", "OTHERS");
                case "joinBucket" -> List.of("NONE", "NESTED_LOOP", "MERGE_JOIN", "HASH_JOIN", "CARTESIAN_JOIN", "OTHERS");
                case "filterBucket" -> List.of("NONE", "ACCESS_FILTER", "POST_FILTER", "JOIN_FILTER", "OTHERS");
                case "sortBucket" -> List.of("NONE", "ORDER_SORT", "GROUP_SORT", "UNIQUE_SORT", "WINDOW_SORT", "OTHERS");
                case "aggregateBucket" -> List.of("NONE", "PLAIN_AGG", "GROUP_AGG", "HASH_AGG", "WINDOW_AGG", "OTHERS");
                default -> List.of();
            };
            case POSTGRESQL -> switch (sectionKey) {
                case "scanBucket" -> List.of("FULL_SCAN", "INDEX_SCAN", "BITMAP_SCAN", "TID_SCAN", "DERIVED_SCAN", "OTHERS");
                case "joinBucket" -> List.of("NONE", "NESTED_LOOP", "MERGE_JOIN", "HASH_JOIN", "OTHERS");
                case "filterBucket" -> List.of("NONE", "ACCESS_FILTER", "POST_FILTER", "JOIN_FILTER", "OTHERS");
                case "sortBucket" -> List.of("NONE", "PLAIN_SORT", "INCREMENTAL_SORT", "OTHERS");
                case "aggregateBucket" -> List.of("NONE", "PLAIN_AGG", "GROUP_AGG", "HASH_AGG", "MIXED_AGG", "WINDOW_AGG", "UNIQUE_AGG", "SET_AGG", "OTHERS");
                default -> List.of();
            };
        };
    }

    private int[] getBucketPlanIndexes(DbmsType dbmsType, String sectionKey, String value) {
        // 버킷 실행 계획 Indexes 조회
        return switch (dbmsType) {
            case ORACLE -> getOracleBucketPlanIndexes(sectionKey, value);
            case POSTGRESQL -> getPostgreSqlBucketPlanIndexes(sectionKey, value);
        };
    }

    private int[] getPostgreSqlBucketPlanIndexes(String sectionKey, String value) {
        // Postgre SQL 버킷 실행 계획 Indexes 조회
        return switch (sectionKey) {
            case "scanBucket" -> switch (value) {
                case "FULL_SCAN" -> new int[]{PostgreSqlExecutionPlanElementIndex.FULL_SCAN, PostgreSqlExecutionPlanElementIndex.SEQ_SCAN};
                case "INDEX_SCAN" -> new int[]{PostgreSqlExecutionPlanElementIndex.INDEX_SCAN, PostgreSqlExecutionPlanElementIndex.INDEX_ONLY_SCAN};
                case "BITMAP_SCAN" -> new int[]{PostgreSqlExecutionPlanElementIndex.BITMAP_INDEX_SCAN, PostgreSqlExecutionPlanElementIndex.BITMAP_HEAP_SCAN};
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

    private int[] getOracleBucketPlanIndexes(String sectionKey, String value) {
        // Oracle 버킷 실행 계획 Indexes 조회
        return switch (sectionKey) {
            case "scanBucket" -> switch (value) {
                case "FULL_SCAN" -> new int[]{OracleExecutionPlanElementIndex.FULL_SCAN};
                case "ROWID_ACCESS" -> new int[]{OracleExecutionPlanElementIndex.ROWID_ACCESS};
                case "INDEX_SCAN" -> new int[]{OracleExecutionPlanElementIndex.INDEX_SCAN};
                case "BITMAP_SCAN" -> new int[]{OracleExecutionPlanElementIndex.BITMAP_SCAN};
                case "DERIVED_SCAN" -> new int[]{OracleExecutionPlanElementIndex.DERIVED_SCAN};
                case "REMOTE_SCAN" -> new int[]{OracleExecutionPlanElementIndex.REMOTE_SCAN};
                default -> new int[0];
            };
            case "joinBucket" -> switch (value) {
                case "NESTED_LOOP" -> new int[]{OracleExecutionPlanElementIndex.NESTED_LOOP};
                case "MERGE_JOIN" -> new int[]{OracleExecutionPlanElementIndex.MERGE_JOIN};
                case "HASH_JOIN" -> new int[]{OracleExecutionPlanElementIndex.HASH_JOIN};
                case "CARTESIAN_JOIN" -> new int[]{OracleExecutionPlanElementIndex.CARTESIAN_JOIN};
                default -> new int[0];
            };
            case "filterBucket" -> switch (value) {
                case "ACCESS_FILTER" -> new int[]{OracleExecutionPlanElementIndex.ACCESS_FILTER};
                case "POST_FILTER" -> new int[]{OracleExecutionPlanElementIndex.POST_FILTER};
                case "JOIN_FILTER" -> new int[]{OracleExecutionPlanElementIndex.JOIN_FILTER};
                default -> new int[0];
            };
            case "sortBucket" -> switch (value) {
                case "ORDER_SORT" -> new int[]{OracleExecutionPlanElementIndex.ORDER_SORT};
                case "GROUP_SORT" -> new int[]{OracleExecutionPlanElementIndex.GROUP_SORT};
                case "UNIQUE_SORT" -> new int[]{OracleExecutionPlanElementIndex.UNIQUE_SORT};
                case "WINDOW_SORT" -> new int[]{OracleExecutionPlanElementIndex.WINDOW_SORT};
                default -> new int[0];
            };
            case "aggregateBucket" -> switch (value) {
                case "PLAIN_AGG" -> new int[]{OracleExecutionPlanElementIndex.PLAIN_AGGREGATE};
                case "GROUP_AGG" -> new int[]{OracleExecutionPlanElementIndex.GROUP_AGGREGATE};
                case "HASH_AGG" -> new int[]{OracleExecutionPlanElementIndex.HASH_AGGREGATE};
                case "WINDOW_AGG" -> new int[]{OracleExecutionPlanElementIndex.WINDOW_AGGREGATE};
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

    private record PlanFilterSelection(PlanMatchMode matchMode,
                                       List<String> scanBuckets,
                                       List<String> joinBuckets,
                                       List<String> filterBuckets,
                                       List<String> sortBuckets,
                                       List<String> aggregateBuckets,
                                       List<String> hintFilters) {
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

    private record PlanFilterSelectionsByDbms(PlanFilterSelection postgresql,
                                              PlanFilterSelection oracle) {
        private PlanFilterSelection get(DbmsType dbmsType) {
            // get 조회
            return dbmsType == DbmsType.ORACLE ? oracle : postgresql;
        }
    }

}
