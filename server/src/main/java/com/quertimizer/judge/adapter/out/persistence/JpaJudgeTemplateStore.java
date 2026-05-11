package com.quertimizer.judge.adapter.out.persistence;

import com.quertimizer.judge.application.port.out.JudgeTemplateStorePort;
import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;
import com.quertimizer.judge.adapter.out.persistence.JudgeDatasetTemplateEntity;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaJudgeTemplateStore implements JudgeTemplateStorePort {

    private final JudgeDatasetTemplateJpaRepository datasetTemplateRepository;

    @Override
    @Transactional
    public void saveDatasetTemplate(DatasetTemplateDefinition templateDefinition) {
        // judge 템플릿 메타데이터를 JPA 엔티티로 변환해 저장
        datasetTemplateRepository.save(JudgeDatasetTemplateEntity.from(templateDefinition));
    }

    @Override
    public Optional<DatasetTemplateDefinition> findDatasetTemplate(JudgeDatasetId datasetId) {
        // JPA 엔티티를 judge 템플릿 정의 객체로 복원
        return datasetTemplateRepository.findById(datasetId.getValue())
                .map(JudgeDatasetTemplateEntity::toDefinition);
    }
}
