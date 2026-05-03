package com.quertimizer.judge.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.judge.application.port.out.JudgeDefinitionStorePort;
import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.adapter.out.persistence.JudgeDatasetDefinitionEntity;
import com.quertimizer.judge.adapter.out.persistence.JudgeSetupSqlDefinitionEntity;
import com.quertimizer.judge.domain.entity.SetupSqlDefinition;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.JudgeSetupSqlId;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.domain.model.IndexPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaJudgeDefinitionStore implements JudgeDefinitionStorePort {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final JudgeDatasetDefinitionJpaRepository datasetRepository;
    private final JudgeSetupSqlDefinitionJpaRepository setupSqlRepository;
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
    public void deleteDataset(JudgeDatasetId datasetId) {
        // 데이터셋 하위 정의 제거 후 데이터셋 정의 제거
        setupSqlRepository.deleteByDatasetId(datasetId.getValue());
        if (datasetRepository.existsById(datasetId.getValue())) {
            datasetRepository.deleteById(datasetId.getValue());
        }
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
