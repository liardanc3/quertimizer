package com.quertimizer.alarm.application.port.in;

import java.util.List;

public interface SearchAlarmRecipientHandlesUseCase {

    List<String> execute(String keyword);
}
