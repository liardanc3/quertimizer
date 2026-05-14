package com.quertimizer.judge.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.judge.application.port.out.DefinitionRepositoryPort;
import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.SetupSqlDefinition;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.JudgeSetupSqlId;
import com.quertimizer.judge.domain.model.IndexPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.quertimizer.judge.domain.model.JudgeFailReason.DEFINITION_DESERIALIZE_FAILED;
import static com.quertimizer.judge.domain.model.JudgeFailReason.DEFINITION_SERIALIZE_FAILED;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefinitionPersistenceAdapter implements DefinitionRepositoryPort {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final JudgeDatasetDefinitionJpaRepository datasetRepository;
    private final JudgeProblemSetDatasetJpaRepository problemSetDatasetRepository;
    private final JudgeSetupSqlDefinitionJpaRepository setupSqlRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public DatasetDefinition saveDataset(DatasetDefinition datasetDefinition, boolean storeSqlDefinition) {
        // 데이터셋 handle과 필요 시 임시 SQL 정의 저장
        JudgeDatasetDefinitionEntity entity = datasetRepository.saveAndFlush(JudgeDatasetDefinitionEntity.from(
                datasetDefinition,
                serializeStringList(datasetDefinition.getBaseIndexDdls()),
                storeSqlDefinition
        ));
        return new DatasetDefinition(
                entity.toDatasetId(), datasetDefinition.getDbmsType(), datasetDefinition.getDdl(),
                datasetDefinition.getDataSql(), datasetDefinition.getBaseIndexDdls()
        );
    }

    @Override
    public Optional<DatasetDefinition> findDataset(JudgeDatasetId datasetId) {
        // 데이터셋 handle 조회 후 임시 SQL 정의 또는 문제셋 원본으로 복원
        return datasetRepository.findById(datasetId.getValue())
                .flatMap(this::toDatasetDefinition);
    }

    @Override
    @Transactional
    public void deleteDataset(JudgeDatasetId datasetId) {
        // 데이터셋 정의 aggregate 삭제로 하위 템플릿과 설정 SQL 함께 제거
        datasetRepository.findById(datasetId.getValue()).ifPresent(datasetRepository::delete);
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
        // 저장된 인덱스 정책 값을 정책 객체로 복원
        return setupSqlRepository.findById(setupSqlId.getValue())
                .map(entity -> new SetupSqlDefinition(
                        entity.toSetupSqlId(),
                        new JudgeDatasetId(entity.getDatasetId()),
                        deserializeStringList(entity.getSetupSqlsJson()),
                        new IndexPolicy(entity.isKeepBaseIndexes(), entity.isApplySetupIndexesOnly())
                ));
    }

    private String serializeStringList(List<String> values) {
        // 문자열 목록 DB 저장용 JSON 변환
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(DEFINITION_SERIALIZE_FAILED.getMessage(), exception);
        }
    }

    private List<String> deserializeStringList(String value) {
        // JSON 문자열을 문자열 목록으로 복원
        try {
            return objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(DEFINITION_DESERIALIZE_FAILED.getMessage(), exception);
        }
    }

    private Optional<DatasetDefinition> toDatasetDefinition(JudgeDatasetDefinitionEntity entity) {
        // 데이터셋 handle에 보관된 기준 인덱스 정의 복원
        List<String> baseIndexDdls = deserializeStringList(entity.getBaseIndexDdlsJson());

        // 임시 데이터셋 SQL 정의가 있으면 우선 사용
        if (entity.getInlineDefinition() != null) {
            return Optional.of(entity.getInlineDefinition().toDefinition(baseIndexDdls));
        }

        // 영속 문제셋 데이터셋은 problem_set 원본 SQL 사용
        return problemSetDatasetRepository.findByDatasetId(entity.getDatasetId())
                .map(problemSet -> problemSet.toDefinition(baseIndexDdls));
    }
}
