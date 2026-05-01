package com.quertimizer.judge.application.usecase;

import com.quertimizer.judge.application.input.CreateJudgeDatasetInput;
import com.quertimizer.judge.application.port.JudgeRuntime;
import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * judge 데이터셋을 생성한다.
 */
@Component
@RequiredArgsConstructor
public class CreateJudgeDataset {

    private final JudgeRuntime judgeRuntime;

    /**
     * judge 데이터셋을 생성한다.
     *
     * @param input 데이터셋 생성 입력
     * @return 생성된 데이터셋 ID
     */
    public JudgeDatasetId execute(CreateJudgeDatasetInput input) {
        return judgeRuntime.createDataset(input);
    }
}
