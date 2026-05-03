package com.quertimizer.user.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;
    private final UserPersistenceMapper userPersistenceMapper;

    @Override
    public Optional<User> findById(String email) {
        return userJpaRepository.findById(email)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmailIgnoreCase(String email) {
        return userJpaRepository.findByEmailIgnoreCase(email)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByHandle(String handle) {
        return userJpaRepository.findByHandle(handle)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public List<User> findTop20ByHandleContainingIgnoreCaseOrderByHandleAsc(String handle) {
        return userJpaRepository.findTop20ByHandleContainingIgnoreCaseOrderByHandleAsc(handle).stream()
                .map(userPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<User> findAllByHandleIn(List<String> handles) {
        return userJpaRepository.findAllByHandleIn(handles).stream()
                .map(userPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByEmailIgnoreCase(String email) {
        return userJpaRepository.existsByEmailIgnoreCase(email);
    }

    @Override
    public boolean existsByHandle(String handle) {
        return userJpaRepository.existsByHandle(handle);
    }

    @Override
    public List<User> findAllByOrderByHandleAsc() {
        return userJpaRepository.findAllByOrderByHandleAsc().stream()
                .map(userPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Page<User> findAllByBlockedUserTrueOrderByBlockedAtDescHandleAsc(Pageable pageable) {
        return userJpaRepository.findAllByBlockedUserTrueOrderByBlockedAtDescHandleAsc(pageable)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserJpaEntity savedEntity = userJpaRepository.findById(user.getEmail())
                .map(entity -> {
                    userPersistenceMapper.updateEntity(entity, user);
                    return entity;
                })
                .orElseGet(() -> userPersistenceMapper.toEntity(user));
        return userPersistenceMapper.toDomain(userJpaRepository.save(savedEntity));
    }

    @Override
    public List<User> saveAll(Iterable<User> users) {
        List<User> savedUsers = new java.util.ArrayList<>();
        users.forEach(user -> savedUsers.add(save(user)));
        return savedUsers;
    }
}
