package com.quertimizer.user.application.port.in;

import java.util.List;

public interface SearchUserHandlesUseCase {

    List<String> execute(String keyword);
}
