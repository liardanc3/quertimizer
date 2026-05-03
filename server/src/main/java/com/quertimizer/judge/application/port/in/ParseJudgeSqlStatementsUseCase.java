package com.quertimizer.judge.application.port.in;

import com.quertimizer.judge.application.output.JudgeSqlStatement;
import java.util.List;

public interface ParseJudgeSqlStatementsUseCase {

    List<JudgeSqlStatement> execute(String sql);
}
