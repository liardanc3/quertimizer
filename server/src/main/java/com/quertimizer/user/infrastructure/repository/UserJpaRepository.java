package com.quertimizer.user.infrastructure.repository;

import com.quertimizer.user.application.port.UserRepository;
import com.quertimizer.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, String>, UserRepository {
}
