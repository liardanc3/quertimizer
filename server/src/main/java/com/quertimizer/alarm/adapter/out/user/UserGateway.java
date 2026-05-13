package com.quertimizer.alarm.adapter.out.user;

import com.quertimizer.alarm.application.port.out.AlarmRecipientPort;
import com.quertimizer.user.application.port.in.FindExistingUserHandlesUseCase;
import com.quertimizer.user.application.port.in.SearchUserHandlesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("alarmUserGateway")
@RequiredArgsConstructor
public class UserGateway implements AlarmRecipientPort {

    private final FindExistingUserHandlesUseCase findExistingUserHandles;
    private final SearchUserHandlesUseCase searchUserHandles;

    @Override
    public List<String> findExistingHandles(List<String> handles) {
        return findExistingUserHandles.execute(handles);
    }

    @Override
    public List<String> searchHandles(String keyword) {
        return searchUserHandles.execute(keyword);
    }

}
