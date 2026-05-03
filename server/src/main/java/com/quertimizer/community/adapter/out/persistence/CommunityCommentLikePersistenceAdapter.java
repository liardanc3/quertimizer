package com.quertimizer.community.adapter.out.persistence;

import java.util.List;
import com.quertimizer.community.application.port.out.CommunityCommentLikeRepositoryPort;
import com.quertimizer.community.domain.entity.CommunityCommentLike;
import com.quertimizer.community.domain.entity.ids.CommunityCommentLikeId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityCommentLikePersistenceAdapter implements CommunityCommentLikeRepositoryPort {

    private final CommunityCommentLikeJpaRepository communityCommentLikeJpaRepository;
    private final CommunityCommentLikePersistenceMapper communityCommentLikePersistenceMapper;

    @Override
    public CommunityCommentLike save(CommunityCommentLike communityCommentLike) {
        CommunityCommentLikeJpaEntity savedEntity = communityCommentLikeJpaRepository.save(
                communityCommentLikePersistenceMapper.toEntity(communityCommentLike)
        );
        return communityCommentLikePersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public List<CommunityCommentLike> saveAll(Iterable<CommunityCommentLike> communityCommentLikes) {
        List<CommunityCommentLikeJpaEntity> entities = new java.util.ArrayList<>();
        communityCommentLikes.forEach(commentLike -> entities.add(communityCommentLikePersistenceMapper.toEntity(commentLike)));
        return communityCommentLikeJpaRepository.saveAll(entities).stream()
                .map(communityCommentLikePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(CommunityCommentLikeId communityCommentLikeId) {
        return communityCommentLikeJpaRepository.existsById(communityCommentLikePersistenceMapper.toJpaId(communityCommentLikeId));
    }

    @Override
    public void deleteById(CommunityCommentLikeId communityCommentLikeId) {
        communityCommentLikeJpaRepository.deleteById(communityCommentLikePersistenceMapper.toJpaId(communityCommentLikeId));
    }

    @Override
    public List<CommunityCommentLike> findAllByIdHandleOrderByCreatedAtDesc(String handle) {
        return communityCommentLikeJpaRepository.findAllByIdHandleOrderByCreatedAtDesc(handle).stream()
                .map(communityCommentLikePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteAllByIdCommentIdIn(List<Long> commentIds) {
        communityCommentLikeJpaRepository.deleteAllByIdCommentIdIn(commentIds);
    }
}
