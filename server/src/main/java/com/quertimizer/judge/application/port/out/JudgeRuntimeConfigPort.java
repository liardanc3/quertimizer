package com.quertimizer.judge.application.port.out;

import com.quertimizer.judge.domain.model.JudgeRuntimeConfig;

import java.util.Optional;

public interface JudgeRuntimeConfigPort {

    Optional<JudgeRuntimeConfig> findRuntimeConfig(String databaseId);
}
