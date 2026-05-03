package com.quertimizer.community.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import com.quertimizer.community.application.port.out.CommunityCommentRepositoryPort;
import com.quertimizer.community.domain.entity.CommunityComment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityCommentPersistenceAdapter implements CommunityCommentRepositoryPort {

    private final CommunityCommentJpaRepository communityCommentJpaRepository;
    private final CommunityCommentPersistenceMapper communityCommentPersistenceMapper;

    @Override
    public List<CommunityComment> findAll() {
        return communityCommentJpaRepository.findAll().stream()
                .map(communityCommentPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<CommunityComment> findById(Long commentId) {
        return communityCommentJpaRepository.findById(commentId)
                .map(communityCommentPersistenceMapper::toDomain);
    }

    @Override
    public CommunityComment save(CommunityComment communityComment) {
        CommunityCommentJpaEntity savedEntity = communityComment.getCommentId() == null
                ? communityCommentPersistenceMapper.toEntity(communityComment)
                : communityCommentJpaRepository.findById(communityComment.getCommentId())
                        .map(entity -> {
                            communityCommentPersistenceMapper.updateEntity(entity, communityComment);
                            return entity;
                        })
                        .orElseGet(() -> communityCommentPersistenceMapper.toEntity(communityComment));
        return communityCommentPersistenceMapper.toDomain(communityCommentJpaRepository.saveAndFlush(savedEntity));
    }

    @Override
    public List<CommunityComment> findAllByPostIdOrderByCreatedAtAsc(Long postId) {
        return communityCommentJpaRepository.findAllByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(communityCommentPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<CommunityComment> findAllByHandleOrderByCreatedAtDesc(String handle) {
        return communityCommentJpaRepository.findAllByHandleOrderByCreatedAtDesc(handle).stream()
                .map(communityCommentPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<CommunityComment> findAllByCommentIdIn(List<Long> commentIds) {
        return communityCommentJpaRepository.findAllByCommentIdIn(commentIds).stream()
                .map(communityCommentPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public long countByHandle(String handle) {
        return communityCommentJpaRepository.countByHandle(handle);
    }

    @Override
    public void deleteAllByPostId(Long postId) {
        communityCommentJpaRepository.deleteAllByPostId(postId);
    }
}
