package com.quertimizer.judge.application.input;

import com.quertimizer.global.constant.DbmsType;

public record InteractiveSqlInput(String handle,
                                  String socketId,
                                  String problemId,
                                  String sql,
                                  DbmsType dbmsType,
                                  Integer page,
                                  Integer pageSize) {
}
