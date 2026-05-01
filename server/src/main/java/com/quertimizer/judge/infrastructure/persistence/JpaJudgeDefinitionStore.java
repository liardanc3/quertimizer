package com.quertimizer.judge.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.ReferenceDefinition;
import com.quertimizer.judge.domain.entity.SetupSqlDefinition;
import com.quertimizer.judge.application.port.JudgeDefinitionStore;
import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.ids.JudgeReferenceId;
import com.quertimizer.judge.domain.entity.ids.JudgeSetupSqlId;
import com.quertimizer.judge.domain.model.IndexPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaJudgeDefinitionStore implements JudgeDefinitionStore {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final JudgeDatasetDefinitionJpaRepository datasetRepository;
    private final JudgeSetupSqlDefinitionJpaRepository setupSqlRepository;
    private final JudgeReferenceDefinitionJpaRepository referenceRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void saveDataset(DatasetDefinition datasetDefinition) {
        // judge 정의 객체 컬렉션 값을 JSON으로 직렬화해 JPA 엔티티 저장
        datasetRepository.save(JudgeDatasetDefinitionEntity.from(
                datasetDefinition,
                serializeStringList(datasetDefinition.getBaseIndexDdls())
        ));
    }

    @Override
    public Optional<DatasetDefinition> findDataset(JudgeDatasetId datasetId) {
        // JPA 엔티티를 judge 정의 객체로 복원
        return datasetRepository.findById(datasetId.getValue())
                .map(entity -> new DatasetDefinition(
                        entity.toDatasetId(),
                        DbmsType.valueOf(entity.getDbmsType()),
                        entity.getDdl(),
                        entity.getDataSql(),
                        deserializeStringList(entity.getBaseIndexDdlsJson())
                ));
    }

    @Override
    @Transactional
    public void saveSetupSql(SetupSqlDefinition setupSqlDefinition) {
        // 설정 SQL 목록과 인덱스 정책 값 분리 저장
        setupSqlRepository.save(JudgeSetupSqlDefinitionEntity.from(
                setupSqlDefinition,
                serializeStringList(setupSqlDefinition.getSetupSqls())
        ));
    }

    @Override
    public Optional<SetupSqlDefinition> findSetupSql(JudgeSetupSqlId setupSqlId) {
        // 저장된 인덱스 정책 값을 judge 정책 객체로 복원
        return setupSqlRepository.findById(setupSqlId.getValue())
                .map(entity -> new SetupSqlDefinition(
                        entity.toSetupSqlId(),
                        new JudgeDatasetId(entity.getDatasetId()),
                        deserializeStringList(entity.getSetupSqlsJson()),
                        new IndexPolicy(entity.isKeepBaseIndexes(), entity.isApplySetupIndexesOnly())
                ));
    }

    @Override
    @Transactional
    public void saveReference(ReferenceDefinition referenceDefinition) {
        // 기준 SQL 원문과 결과 해시 저장
        referenceRepository.save(JudgeReferenceDefinitionEntity.from(referenceDefinition));
    }

    @Override
    public Optional<ReferenceDefinition> findReference(JudgeReferenceId referenceId) {
        // 저장된 기준 SQL 정의를 judge 정의 객체로 복원
        return referenceRepository.findById(referenceId.getValue())
                .map(entity -> new ReferenceDefinition(
                        entity.toReferenceId(),
                        new JudgeDatasetId(entity.getDatasetId()),
                        entity.getReferenceSql(),
                        entity.getResultHash()
                ));
    }

    private String serializeStringList(List<String> values) {
        // 문자열 목록 DB 저장용 JSON 변환
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("judge 정의 직렬화에 실패했다.", exception);
        }
    }

    private List<String> deserializeStringList(String value) {
        // JSON 문자열을 문자열 목록으로 복원
        try {
            return objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("judge 정의 역직렬화에 실패했다.", exception);
        }
    }
}
