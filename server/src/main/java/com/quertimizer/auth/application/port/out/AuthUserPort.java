package com.quertimizer.auth.application.port.out;

import com.quertimizer.auth.domain.model.AuthUser;

import java.util.List;
import java.util.Optional;

public interface AuthUserPort {

    Optional<AuthUser> findById(String email);

    Optional<AuthUser> findByEmail(String email);

    Optional<AuthUser> findByEmailIgnoreCase(String email);

    Optional<AuthUser> findByHandle(String handle);

    List<AuthUser> findAllByOrderByHandleAsc();

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByHandle(String handle);

    AuthUser save(AuthUser user);

}
