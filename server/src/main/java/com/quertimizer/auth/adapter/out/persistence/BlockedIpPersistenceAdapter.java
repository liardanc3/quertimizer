package com.quertimizer.auth.adapter.out.persistence;

import java.util.Optional;
import com.quertimizer.auth.application.port.out.BlockedIpRepositoryPort;
import com.quertimizer.auth.domain.entity.BlockedIp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlockedIpPersistenceAdapter implements BlockedIpRepositoryPort {

    private final BlockedIpJpaRepository blockedIpJpaRepository;
    private final BlockedIpPersistenceMapper blockedIpPersistenceMapper;

    @Override
    public boolean existsByIpAddress(String ipAddress) {
        return blockedIpJpaRepository.existsByIpAddress(ipAddress);
    }

    @Override
    public void deleteByBlockedHandle(String blockedHandle) {
        blockedIpJpaRepository.deleteByBlockedHandle(blockedHandle);
    }

    @Override
    public Page<BlockedIp> findAllByOrderByBlockedAtDescIpAddressAsc(Pageable pageable) {
        return blockedIpJpaRepository.findAllByOrderByBlockedAtDescIpAddressAsc(pageable)
                .map(blockedIpPersistenceMapper::toDomain);
    }

    @Override
    public Optional<BlockedIp> findById(String ipAddress) {
        return blockedIpJpaRepository.findById(ipAddress)
                .map(blockedIpPersistenceMapper::toDomain);
    }

    @Override
    public BlockedIp save(BlockedIp blockedIp) {
        BlockedIpJpaEntity savedEntity = blockedIpJpaRepository.findById(blockedIp.getIpAddress())
                .map(entity -> {
                    blockedIpPersistenceMapper.updateEntity(entity, blockedIp);
                    return entity;
                })
                .orElseGet(() -> blockedIpPersistenceMapper.toEntity(blockedIp));
        return blockedIpPersistenceMapper.toDomain(blockedIpJpaRepository.save(savedEntity));
    }

    @Override
    public void deleteById(String ipAddress) {
        blockedIpJpaRepository.deleteById(ipAddress);
    }
}
