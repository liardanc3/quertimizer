package com.quertimizer.problem.adapter.out.persistence;

import com.quertimizer.problem.application.port.out.ProblemSubmitHistoryRepositoryPort;
import com.quertimizer.problem.application.port.out.SubmitHistorySearchCondition;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import com.quertimizer.problem.domain.model.SubmitHistoryPageConstant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Component
@RequiredArgsConstructor
public class ProblemSubmitHistoryPersistenceAdapter implements ProblemSubmitHistoryRepositoryPort {

    private final ProblemSubmitHistoryJpaRepository problemSubmitHistoryJpaRepository;
    private final ProblemSubmitHistoryPersistenceMapper problemSubmitHistoryPersistenceMapper;
    private final EntityManager entityManager;

    @Override
    public List<ProblemSubmitHistory> findAll() {
        return problemSubmitHistoryJpaRepository.findAll().stream()
                .map(problemSubmitHistoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ProblemSubmitHistory> findAllByHandleOrderBySubmittedAtDesc(String handle) {
        return problemSubmitHistoryJpaRepository.findAllByHandleOrderBySubmittedAtDesc(handle).stream()
                .map(problemSubmitHistoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> findDistinctProblemIds() {
        return entityManager.createNativeQuery("select distinct problem_id from problem_submit_history")
                .getResultList().stream()
                .map(String.class::cast)
                .toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<ProblemSubmitHistory> search(SubmitHistorySearchCondition condition, Pageable pageable, String costSort) {
        // 검색 조건과 페이징 쿼리 생성
        Map<String, Object> parameters = new LinkedHashMap<>();
        String whereClause = createSearchWhereClause(condition, parameters);
        Query query = entityManager.createNativeQuery(
                "select * from problem_submit_history" + whereClause + createOrderByClause(costSort),
                ProblemSubmitHistoryJpaEntity.class
        );
        Query countQuery = entityManager.createNativeQuery(
                "select count(*) from problem_submit_history" + whereClause
        );

        // 검색 파라미터 적용과 페이지 범위 지정
        applyParameters(query, parameters);
        applyParameters(countQuery, parameters);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        // 조회 결과를 도메인 페이지로 변환
        List<ProblemSubmitHistory> histories = ((List<ProblemSubmitHistoryJpaEntity>) query.getResultList()).stream()
                .map(problemSubmitHistoryPersistenceMapper::toDomain)
                .toList();
        long totalCount = ((Number) countQuery.getSingleResult()).longValue();
        return new PageImpl<>(histories, pageable, totalCount);
    }

    @Override
    public ProblemSubmitHistory save(ProblemSubmitHistory problemSubmitHistory) {
        ProblemSubmitHistoryJpaEntity savedEntity = problemSubmitHistoryJpaRepository.save(
                problemSubmitHistoryPersistenceMapper.toEntity(problemSubmitHistory)
        );
        return problemSubmitHistoryPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Page<UserSubmitCountProjection> findUserSubmitCounts(Pageable pageable) {
        return problemSubmitHistoryJpaRepository.findUserSubmitCounts(pageable)
                .map(projection -> new UserSubmitCount(projection.getHandle(), projection.getSubmitCount()));
    }

    @Override
    public Page<UserSubmitCountProjection> findUserSubmitCountsSince(LocalDateTime submittedAfter, Pageable pageable) {
        return problemSubmitHistoryJpaRepository.findUserSubmitCountsSince(submittedAfter, pageable)
                .map(projection -> new UserSubmitCount(projection.getHandle(), projection.getSubmitCount()));
    }

    @Override
    public Page<UserSubmitCountProjection> findUserSubmitCountsBetween(LocalDateTime submittedStart,
                                                                       LocalDateTime submittedEnd,
                                                                       Pageable pageable) {
        return problemSubmitHistoryJpaRepository.findUserSubmitCountsBetween(submittedStart, submittedEnd, pageable)
                .map(projection -> new UserSubmitCount(projection.getHandle(), projection.getSubmitCount()));
    }

    private static final class UserSubmitCount implements UserSubmitCountProjection {
        private final String handle;
        private final long submitCount;

        private UserSubmitCount(String handle, long submitCount) {
            this.handle = handle;
            this.submitCount = submitCount;
        }

        @Override
        public String getHandle() {
            return handle;
        }

        @Override
        public long getSubmitCount() {
            return submitCount;
        }
    }

    private String createSearchWhereClause(SubmitHistorySearchCondition condition, Map<String, Object> parameters) {
        // 기본 제출 이력 검색 조건 생성
        StringBuilder whereClause = new StringBuilder(" where 1 = 1");
        appendSubmitIdCondition(whereClause, parameters, condition.getSubmitId());
        appendTextCondition(whereClause, parameters, "handle", condition.getQuery(), "query");
        appendDbmsCondition(whereClause, parameters, condition);
        appendTextCondition(whereClause, parameters, "problem_id", condition.getProblemId(), "problemId");
        appendSuccessCondition(whereClause, parameters, condition.getSuccess());
        appendPlanCondition(whereClause, parameters, condition);
        return whereClause.toString();
    }

    private void appendSubmitIdCondition(StringBuilder whereClause, Map<String, Object> parameters, String submitId) {
        // 제출 번호 검색 조건 추가
        if (submitId == null || submitId.isBlank()) {
            return;
        }

        whereClause.append("""
                 and (
                    cast(submit_id as varchar) like :submitId
                    or lpad(cast(submit_id as varchar), :submitIdLength, '0') like :submitId
                )
                """);
        parameters.put("submitId", "%" + submitId.trim() + "%");
        parameters.put("submitIdLength", SubmitHistoryPageConstant.SUBMIT_ID_LENGTH);
    }

    private void appendTextCondition(StringBuilder whereClause, Map<String, Object> parameters,
                                     String columnName, String value, String parameterName) {
        // 문자열 포함 검색 조건 추가
        if (value == null || value.isBlank()) {
            return;
        }

        whereClause.append(" and lower(")
                .append(columnName)
                .append(") like :")
                .append(parameterName);
        parameters.put(parameterName, "%" + value.trim().toLowerCase() + "%");
    }

    private void appendDbmsCondition(StringBuilder whereClause, Map<String, Object> parameters,
                                     SubmitHistorySearchCondition condition) {
        // DBMS 검색 조건 추가
        if (condition.getDbmsType() == null) {
            return;
        }

        if (condition.getDbmsType().name().equals("POSTGRESQL")) {
            whereClause.append(" and (dbms_type = :dbmsType or dbms_type is null)");
        } else {
            whereClause.append(" and dbms_type = :dbmsType");
        }
        parameters.put("dbmsType", condition.getDbmsType().name());
    }

    private void appendSuccessCondition(StringBuilder whereClause, Map<String, Object> parameters, Boolean success) {
        // 채점 결과 검색 조건 추가
        if (success == null) {
            return;
        }

        whereClause.append(" and success = :success");
        parameters.put("success", success);
    }

    private void appendPlanCondition(StringBuilder whereClause, Map<String, Object> parameters,
                                     SubmitHistorySearchCondition condition) {
        // 실행 계획 검색 조건 추가
        if (!condition.hasPlanCondition()) {
            return;
        }

        StringJoiner dbmsConditions = new StringJoiner(" or ");
        if (condition.getDbmsType() == null || condition.getDbmsType().name().equals("POSTGRESQL")) {
            dbmsConditions.add("(coalesce(dbms_type, 'POSTGRESQL') = 'POSTGRESQL' and "
                    + createPlanElementCondition(condition.getPostgresqlPlanCondition(), parameters) + ")");
        }
        if (condition.getDbmsType() == null || condition.getDbmsType().name().equals("MYSQL")) {
            dbmsConditions.add("(dbms_type = 'MYSQL' and "
                    + createPlanElementCondition(condition.getMysqlPlanCondition(), parameters) + ")");
        }

        whereClause.append(" and (").append(dbmsConditions).append(")");
    }

    private String createPlanElementCondition(SubmitHistorySearchCondition.PlanElementSearchCondition condition,
                                              Map<String, Object> parameters) {
        // 실행 계획 비트 조건 생성
        if (!condition.hasConditions()) {
            return "1 = 1";
        }

        StringJoiner conditions = new StringJoiner(condition.matchAll() ? " and " : " or ");
        for (SubmitHistorySearchCondition.PlanElementCondition elementCondition : condition.conditions()) {
            String parameterName = "planMask" + parameters.size();
            String operator = elementCondition.matched() ? "<>" : "=";
            conditions.add("(coalesce(execution_plan_element, 0) & :" + parameterName + ") " + operator + " 0");
            parameters.put(parameterName, elementCondition.mask());
        }

        return conditions.toString();
    }

    private String createOrderByClause(String costSort) {
        // 제출 이력 정렬 조건 생성
        if ("asc".equalsIgnoreCase(costSort)) {
            return " order by success desc, cost asc, submitted_at desc, submit_id desc";
        }

        if ("desc".equalsIgnoreCase(costSort)) {
            return " order by success desc, cost desc, submitted_at desc, submit_id desc";
        }

        return " order by submitted_at desc, submit_id desc";
    }

    private void applyParameters(Query query, Map<String, Object> parameters) {
        // Native query 파라미터 적용
        parameters.forEach(query::setParameter);
    }
}
