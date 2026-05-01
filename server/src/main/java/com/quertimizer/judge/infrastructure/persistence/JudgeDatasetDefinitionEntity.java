package com.quertimizer.judge.infrastructure.persistence;

import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sql_judge_dataset_definition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JudgeDatasetDefinitionEntity {

    @Id
    @Column(name = "dataset_id", nullable = false, length = 80)
    private String datasetId;

    @Column(name = "dbms_type", nullable = false, length = 20)
    private String dbmsType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String ddl;

    @Column(name = "data_sql", nullable = false, columnDefinition = "TEXT")
    private String dataSql;

    @Column(name = "base_index_ddls_json", nullable = false, columnDefinition = "TEXT")
    private String baseIndexDdlsJson;

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "datasetDefinition")
    private List<JudgeReferenceDefinitionEntity> referenceDefinitions = new ArrayList<>();

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "datasetDefinition")
    private List<JudgeSetupSqlDefinitionEntity> setupSqlDefinitions = new ArrayList<>();

    @OneToOne(mappedBy = "datasetDefinition")
    private JudgeDatasetTemplateEntity templateDefinition;

    public static JudgeDatasetDefinitionEntity from(DatasetDefinition definition, String baseIndexDdlsJson) {
        return new JudgeDatasetDefinitionEntity(
                definition.getDatasetId().getValue(),
                definition.getDbmsType().name(),
                definition.getDdl(),
                definition.getDataSql(),
                baseIndexDdlsJson
        );
    }

    public JudgeDatasetId toDatasetId() {
        return new JudgeDatasetId(datasetId);
    }

    private JudgeDatasetDefinitionEntity(String datasetId, String dbmsType, String ddl,
                                            String dataSql, String baseIndexDdlsJson) {
        this.datasetId = datasetId;
        this.dbmsType = dbmsType;
        this.ddl = ddl;
        this.dataSql = dataSql;
        this.baseIndexDdlsJson = baseIndexDdlsJson;
    }
}
