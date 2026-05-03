package com.quertimizer.user.adapter.out.persistence;

import java.util.List;
import com.quertimizer.user.application.port.out.UserExternalLinkRepositoryPort;
import com.quertimizer.user.domain.entity.UserExternalLink;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserExternalLinkPersistenceAdapter implements UserExternalLinkRepositoryPort {

    private final UserExternalLinkJpaRepository userExternalLinkJpaRepository;
    private final UserExternalLinkPersistenceMapper userExternalLinkPersistenceMapper;

    @Override
    public List<UserExternalLink> findAllByIdHandleOrderByIdTypeAscIdLinkAsc(String handle) {
        return userExternalLinkJpaRepository.findAllByIdHandleOrderByIdTypeAscIdLinkAsc(handle).stream()
                .map(userExternalLinkPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteAllByIdHandle(String handle) {
        userExternalLinkJpaRepository.deleteAllByIdHandle(handle);
    }

    @Override
    public List<UserExternalLink> saveAll(Iterable<UserExternalLink> userExternalLinks) {
        List<UserExternalLinkJpaEntity> entities = new java.util.ArrayList<>();
        userExternalLinks.forEach(link -> entities.add(userExternalLinkPersistenceMapper.toEntity(link)));
        return userExternalLinkJpaRepository.saveAll(entities).stream()
                .map(userExternalLinkPersistenceMapper::toDomain)
                .toList();
    }
}
