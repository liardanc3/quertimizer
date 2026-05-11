package com.quertimizer.community.adapter.out.persistence;

import java.util.List;
import com.quertimizer.community.application.port.out.CommunityPostTagRepositoryPort;
import com.quertimizer.community.domain.entity.CommunityPostTag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityPostTagPersistenceAdapter implements CommunityPostTagRepositoryPort {

    private final CommunityPostTagJpaRepository communityPostTagJpaRepository;
    private final CommunityPostTagPersistenceMapper communityPostTagPersistenceMapper;

    @Override
    public List<CommunityPostTag> findAllByPostIdOrderByTagOrderAsc(Long postId) {
        return communityPostTagJpaRepository.findAllByPostIdOrderByTagOrderAsc(postId).stream()
                .map(communityPostTagPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<CommunityPostTag> findAllByPostIdInOrderByPostIdAscTagOrderAsc(List<Long> postIds) {
        return communityPostTagJpaRepository.findAllByPostIdInOrderByPostIdAscTagOrderAsc(postIds).stream()
                .map(communityPostTagPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteAllByPostId(Long postId) {
        communityPostTagJpaRepository.deleteAllByPostId(postId);
    }

    @Override
    public List<CommunityPostTag> findAllByTagOrderByPostIdAscTagOrderAsc(String tag) {
        return communityPostTagJpaRepository.findAllByTagOrderByPostIdAscTagOrderAsc(tag).stream()
                .map(communityPostTagPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<CommunityPostTag> findAllByTagContainingIgnoreCaseOrderByTagAsc(String tag) {
        return communityPostTagJpaRepository.findAllByTagContainingIgnoreCaseOrderByTagAsc(tag).stream()
                .map(communityPostTagPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<CommunityPostTag> saveAll(Iterable<CommunityPostTag> communityPostTags) {
        List<CommunityPostTagJpaEntity> entities = new java.util.ArrayList<>();
        communityPostTags.forEach(postTag -> entities.add(communityPostTagPersistenceMapper.toEntity(postTag)));
        return communityPostTagJpaRepository.saveAll(entities).stream()
                .map(communityPostTagPersistenceMapper::toDomain)
                .toList();
    }
}
