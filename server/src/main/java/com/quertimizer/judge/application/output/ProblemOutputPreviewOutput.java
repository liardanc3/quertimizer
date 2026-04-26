package com.quertimizer.judge.application.output;

import java.util.List;

public record ProblemOutputPreviewOutput(List<String> columns,
                                         List<List<String>> rows,
                                         long rowCount) {
}
