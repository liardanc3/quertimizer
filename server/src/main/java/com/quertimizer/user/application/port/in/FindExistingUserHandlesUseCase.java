package com.quertimizer.user.application.port.in;

import java.util.List;

public interface FindExistingUserHandlesUseCase {

    List<String> execute(List<String> handles);
}
