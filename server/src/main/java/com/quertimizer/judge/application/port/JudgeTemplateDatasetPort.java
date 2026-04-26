package com.quertimizer.judge.application.port;

import com.quertimizer.judge.application.input.RefreshTemplateDatasetInput;

public interface JudgeTemplateDatasetPort {

    void refreshTemplateDataset(RefreshTemplateDatasetInput input);
}
