package com.quertimizer.community.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import com.quertimizer.community.application.port.out.CommunityPostRepositoryPort;
import com.quertimizer.community.domain.entity.CommunityPost;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityPostPersistenceAdapter implements CommunityPostRepositoryPort {

    private final CommunityPostJpaRepository communityPostJpaRepository;
    private final CommunityPostPersistenceMapper communityPostPersistenceMapper;

    @Override
    public List<CommunityPost> findAll() {
        return communityPostJpaRepository.findAll().stream()
                .map(communityPostPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<CommunityPost> findById(Long postId) {
        return communityPostJpaRepository.findById(postId)
                .map(communityPostPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Long> findTopPostId() {
        return communityPostJpaRepository.findTopPostId();
    }

    @Override
    public CommunityPost save(CommunityPost communityPost) {
        CommunityPostJpaEntity savedEntity = communityPostJpaRepository.findById(communityPost.getPostId())
                .map(entity -> {
                    communityPostPersistenceMapper.updateEntity(entity, communityPost);
                    return entity;
                })
                .orElseGet(() -> communityPostPersistenceMapper.toEntity(communityPost));
        return communityPostPersistenceMapper.toDomain(communityPostJpaRepository.save(savedEntity));
    }

    @Override
    public List<CommunityPost> saveAll(Iterable<CommunityPost> communityPosts) {
        List<CommunityPost> savedPosts = new java.util.ArrayList<>();
        communityPosts.forEach(communityPost -> savedPosts.add(save(communityPost)));
        return savedPosts;
    }

    @Override
    public void delete(CommunityPost communityPost) {
        communityPostJpaRepository.deleteById(communityPost.getPostId());
    }

    @Override
    public List<CommunityPost> findAllByPostIdIn(List<Long> postIds) {
        return communityPostJpaRepository.findAllByPostIdIn(postIds).stream()
                .map(communityPostPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<CommunityPost> findAllByHandleOrderByCreatedAtDesc(String handle) {
        return communityPostJpaRepository.findAllByHandleOrderByCreatedAtDesc(handle).stream()
                .map(communityPostPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public long countByHandle(String handle) {
        return communityPostJpaRepository.countByHandle(handle);
    }
}
