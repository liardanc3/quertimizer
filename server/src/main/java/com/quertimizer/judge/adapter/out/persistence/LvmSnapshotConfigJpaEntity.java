package com.quertimizer.judge.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "lvm_snapshot_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LvmSnapshotConfigJpaEntity {

    @Id
    @Column(name = "config_id", nullable = false, length = 80)
    private String configId;

    @Column(name = "mount_root", nullable = false, length = 200)
    private String mountRoot;

    @Column(name = "volume_group", nullable = false, length = 120)
    private String volumeGroup;

    @Column(name = "thin_pool", nullable = false, length = 120)
    private String thinPool;

    @Column(name = "base_template_version", nullable = false, length = 120)
    private String baseTemplateVersion;

    @Column(name = "command_timeout_seconds", nullable = false)
    private int commandTimeoutSeconds;

    @Column(name = "startup_timeout_seconds", nullable = false)
    private int startupTimeoutSeconds;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
