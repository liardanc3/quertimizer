package com.quertimizer.judge.application.service;

import com.quertimizer.judge.application.port.in.CreateJudgeDatasetUseCase;
import com.quertimizer.judge.application.input.CreateDataset;
import com.quertimizer.judge.application.port.out.JudgeRuntimePort;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateJudgeDataset implements CreateJudgeDatasetUseCase {

    private final JudgeRuntimePort judgeRuntime;

    /**
     * judge 데이터셋을 생성한다.
     *
     * @param input 데이터셋 생성 입력
     * @return 생성된 데이터셋 ID
     */
    @Override
    public JudgeDatasetId execute(CreateDataset input) {
        return judgeRuntime.createDataset(input);
    }
}
