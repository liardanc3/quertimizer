package com.quertimizer.community.adapter.out.persistence;

import java.util.List;
import com.quertimizer.community.application.port.out.CommunityPostLikeRepositoryPort;
import com.quertimizer.community.domain.entity.CommunityPostLike;
import com.quertimizer.community.domain.entity.ids.CommunityPostLikeId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityPostLikePersistenceAdapter implements CommunityPostLikeRepositoryPort {

    private final CommunityPostLikeJpaRepository communityPostLikeJpaRepository;
    private final CommunityPostLikePersistenceMapper communityPostLikePersistenceMapper;

    @Override
    public CommunityPostLike save(CommunityPostLike communityPostLike) {
        CommunityPostLikeJpaEntity savedEntity = communityPostLikeJpaRepository.save(
                communityPostLikePersistenceMapper.toEntity(communityPostLike)
        );
        return communityPostLikePersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public List<CommunityPostLike> saveAll(Iterable<CommunityPostLike> communityPostLikes) {
        List<CommunityPostLikeJpaEntity> entities = new java.util.ArrayList<>();
        communityPostLikes.forEach(postLike -> entities.add(communityPostLikePersistenceMapper.toEntity(postLike)));
        return communityPostLikeJpaRepository.saveAll(entities).stream()
                .map(communityPostLikePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(CommunityPostLikeId communityPostLikeId) {
        return communityPostLikeJpaRepository.existsById(communityPostLikePersistenceMapper.toJpaId(communityPostLikeId));
    }

    @Override
    public void deleteById(CommunityPostLikeId communityPostLikeId) {
        communityPostLikeJpaRepository.deleteById(communityPostLikePersistenceMapper.toJpaId(communityPostLikeId));
    }

    @Override
    public void deleteAllByIdPostId(Long postId) {
        communityPostLikeJpaRepository.deleteAllByIdPostId(postId);
    }

    @Override
    public List<CommunityPostLike> findAllByIdHandleOrderByCreatedAtDesc(String handle) {
        return communityPostLikeJpaRepository.findAllByIdHandleOrderByCreatedAtDesc(handle).stream()
                .map(communityPostLikePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public long countByIdHandle(String handle) {
        return communityPostLikeJpaRepository.countByIdHandle(handle);
    }
}
