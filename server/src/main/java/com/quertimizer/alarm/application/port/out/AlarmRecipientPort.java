package com.quertimizer.alarm.application.port.out;

import java.util.List;

public interface AlarmRecipientPort {

    List<String> findExistingHandles(List<String> handles);

    List<String> searchHandles(String keyword);

}
