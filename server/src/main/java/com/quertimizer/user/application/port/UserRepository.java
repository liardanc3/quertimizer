package com.quertimizer.user.application.port;

import com.quertimizer.user.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByHandle(String handle);

    List<User> findTop20ByHandleContainingIgnoreCaseOrderByHandleAsc(String handle);

    List<User> findAllByHandleIn(List<String> handles);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByHandle(String handle);

    List<User> findAllByOrderByHandleAsc();

    Page<User> findAllByBlockedUserTrueOrderByBlockedAtDescHandleAsc(Pageable pageable);

    <S extends User> S save(S user);

    <S extends User> List<S> saveAll(Iterable<S> users);
}
