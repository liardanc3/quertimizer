package com.quertimizer.alarm.application.port.in;

import com.quertimizer.user.domain.entity.User;
import java.util.List;

public interface SearchAlarmRecipientHandlesUseCase {

    List<String> execute(String keyword);
}
