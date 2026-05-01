package com.quertimizer.judge.infrastructure.config;

import com.quertimizer.global.constant.DbmsType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Optional;

@Getter
@Setter
@ConfigurationProperties(prefix = "judge")
public class JudgeProperties {

    private RuntimeProperties runtime = new RuntimeProperties();
    private List<DatabaseProperties> databases = List.of();

    public RuntimeProperties getRuntime() {
        // 설정 누락 시 기본 SQL 재실행 준비 방식 사용용 빈 설정 객체 제공
        return runtime != null ? runtime : new RuntimeProperties();
    }

    public List<DatabaseProperties> getDatabases() {
        // 설정 누락 시에도 judge 빈 생성 단계에서 null 참조가 나지 않도록 빈 목록 정리
        return databases != null ? databases : List.of();
    }

    @Getter
    @Setter
    public static class RuntimeProperties {
        private String provisioner = "sql-replay";
        private LvmSnapshotProperties lvmSnapshot = new LvmSnapshotProperties();

        public String getProvisioner() {
            // 빈 설정값은 현재 구현 완료된 SQL 재실행 방식으로 보정
            return provisioner != null && !provisioner.isBlank() ? provisioner.trim() : "sql-replay";
        }

        public LvmSnapshotProperties getLvmSnapshot() {
            // 설정 누락 시에도 기본 마운트 루트와 제한 시간을 사용해 빈 생성 허용
            return lvmSnapshot != null ? lvmSnapshot : new LvmSnapshotProperties();
        }
    }

    @Getter
    @Setter
    public static class LvmSnapshotProperties {
        private String mountRoot = "/mnt/sqljudge";
        private String volumeGroup = "vg_sqljudge";
        private String thinPool = "pool";
        private String baseTemplateVersion = "base";
        private int commandTimeoutSeconds = 60;
        private int startupTimeoutSeconds = 30;

        public String getMountRoot() {
            // 빈 설정값은 운영 컨테이너와 공유하는 기본 마운트 루트로 보정
            return mountRoot != null && !mountRoot.isBlank() ? mountRoot.trim() : "/mnt/sqljudge";
        }

        public String getVolumeGroup() {
            // 빈 설정값은 운영 초기화 기본 VG 이름으로 보정
            return volumeGroup != null && !volumeGroup.isBlank() ? volumeGroup.trim() : "vg_sqljudge";
        }

        public String getThinPool() {
            // 빈 설정값은 운영 초기화 기본 thin pool 이름으로 보정
            return thinPool != null && !thinPool.isBlank() ? thinPool.trim() : "pool";
        }

        public String getBaseTemplateVersion() {
            // 빈 설정값은 운영 초기화에서 준비할 기본 템플릿 버전으로 보정
            return baseTemplateVersion != null && !baseTemplateVersion.isBlank() ? baseTemplateVersion.trim() : "base";
        }

        public int getCommandTimeoutSeconds() {
            // 잘못된 제한 시간 설정은 프로세스가 무기한 대기하지 않도록 기본값으로 보정
            return commandTimeoutSeconds > 0 ? commandTimeoutSeconds : 60;
        }

        public int getStartupTimeoutSeconds() {
            // 잘못된 시작 제한 시간 설정은 즉시 실패하지 않도록 기본값으로 보정
            return startupTimeoutSeconds > 0 ? startupTimeoutSeconds : 30;
        }
    }

    @Getter
    @Setter
    public static class DatabaseProperties {
        private String id;
        private String name;
        private String engine;
        private String dbmsType;
        private String url;
        private String username;
        private String password;
        private String runnerContainer;
        private String runtimeHost;
        private Integer runtimePortStart;
        private Integer runtimePortEnd;
        private String runtimeDatabaseName;
        private String rootPassword;
        private boolean enabled = true;
        private int maxConcurrency = 1;
        private Integer weight;

        public Optional<DbmsType> resolveEngine() {
            // engine과 dbmsType 설정 키를 모두 허용해 설정 전환 비용 절감
            return DbmsType.fromValue(engine != null && !engine.isBlank() ? engine : dbmsType);
        }
    }
}
