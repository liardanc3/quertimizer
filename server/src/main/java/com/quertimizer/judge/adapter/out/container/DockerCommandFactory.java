package com.quertimizer.judge.adapter.out.container;

import com.quertimizer.judge.application.model.Constants;
import com.quertimizer.judge.application.model.Options;
import com.quertimizer.judge.application.model.Names;
import com.quertimizer.judge.domain.model.DbmsType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DockerCommandFactory {

    private final Options options;

    public List<String> startEvalProcessCommand(DbmsType dbmsType, String containerName,
                                                String environmentId, int port) {
        // 평가 snapshot을 바라보는 컨테이너 내부 DB 프로세스 시작 명령 반환
        String normalizedEnvironmentId = scriptName(environmentId);
        return switch (dbmsType) {
            case POSTGRESQL -> startPostgresProcessCommand(
                    containerName, evalMountPath("postgresql", normalizedEnvironmentId),
                    evalLogPath("postgresql", normalizedEnvironmentId) + "/postgres.log", port
            );
            case MYSQL -> startMysqlProcessCommand(
                    containerName, evalMountPath("mysql", normalizedEnvironmentId),
                    evalLogPath("mysql", normalizedEnvironmentId) + "/mysql.log",
                    mysqlSocketPath(normalizedEnvironmentId), mysqlPidPath(normalizedEnvironmentId), port
            );
        };
    }

    public List<String> stopEvalProcessCommand(DbmsType dbmsType, String containerName,
                                               String environmentId, String mysqlRootPassword) {
        // 평가 snapshot을 바라보는 컨테이너 내부 DB 프로세스 정지 명령 반환
        String normalizedEnvironmentId = scriptName(environmentId);
        return switch (dbmsType) {
            case POSTGRESQL -> stopPostgresProcessCommand(
                    containerName, evalMountPath("postgresql", normalizedEnvironmentId)
            );
            case MYSQL -> stopMysqlProcessCommand(
                    containerName, mysqlSocketPath(normalizedEnvironmentId), mysqlRootPassword
            );
        };
    }

    public List<String> startTemplateProcessCommand(DbmsType dbmsType, String containerName,
                                                    String templateVersion, int port) {
        // 템플릿 snapshot을 바라보는 컨테이너 내부 DB 프로세스 시작 명령 반환
        String normalizedTemplateVersion = scriptName(templateVersion);
        return switch (dbmsType) {
            case POSTGRESQL -> startPostgresProcessCommand(
                    containerName, templateMountPath("postgresql", normalizedTemplateVersion),
                    templateLogPath("postgresql", normalizedTemplateVersion) + "/postgres.log", port
            );
            case MYSQL -> startMysqlProcessCommand(
                    containerName, templateMountPath("mysql", normalizedTemplateVersion),
                    templateLogPath("mysql", normalizedTemplateVersion) + "/mysql.log",
                    mysqlTemplateSocketPath(normalizedTemplateVersion),
                    mysqlTemplatePidPath(normalizedTemplateVersion), port
            );
        };
    }

    public List<String> stopTemplateProcessCommand(DbmsType dbmsType, String containerName,
                                                   String templateVersion, String mysqlRootPassword) {
        // 템플릿 snapshot을 바라보는 컨테이너 내부 DB 프로세스 정지 명령 반환
        String normalizedTemplateVersion = scriptName(templateVersion);
        return switch (dbmsType) {
            case POSTGRESQL -> stopPostgresProcessCommand(
                    containerName, templateMountPath("postgresql", normalizedTemplateVersion)
            );
            case MYSQL -> stopMysqlProcessCommand(
                    containerName, mysqlTemplateSocketPath(normalizedTemplateVersion), mysqlRootPassword
            );
        };
    }

    private List<String> startPostgresProcessCommand(String containerName, String dataDir,
                                                     String logPath, int port) {
        return List.of(
                "docker", "exec", "--user", Constants.POSTGRES_USER,
                containerName.trim(),
                "sh", "-lc",
                Constants.POSTGRES_CTL_RESOLVER
                        + "\"$pg_ctl_bin\" -w -t " + Constants.POSTGRES_CTL_TIMEOUT_SECONDS
                        + " -D " + shellQuote(dataDir)
                        + " -o " + shellQuote(
                                "-p " + port + " -c listen_addresses=0.0.0.0 "
                                        + "-c shared_preload_libraries=pg_hint_plan"
                                        + " -c autovacuum=off"
                                        + " -c default_statistics_target="
                                        + Constants.POSTGRES_DEFAULT_STATISTICS_TARGET
                                        + " -c jit=off"
                                        + " -c seq_page_cost=1.0"
                                        + " -c random_page_cost=4.0"
                                        + " -c cpu_tuple_cost=0.01"
                                        + " -c cpu_index_tuple_cost=0.005"
                                        + " -c cpu_operator_cost=0.0025"
                        )
                        + " -l " + shellQuote(logPath) + " start"
        );
    }

    private List<String> stopPostgresProcessCommand(String containerName, String dataDir) {
        return List.of(
                "docker", "exec", "--user", Constants.POSTGRES_USER,
                containerName.trim(),
                "sh", "-lc",
                Constants.POSTGRES_CTL_RESOLVER
                        + "\"$pg_ctl_bin\" -w -t " + Constants.POSTGRES_CTL_TIMEOUT_SECONDS
                        + " -D " + shellQuote(dataDir) + " -m fast stop"
        );
    }

    private List<String> startMysqlProcessCommand(String containerName, String dataDir, String logPath,
                                                  String socketPath, String pidPath, int port) {
        return List.of(
                "docker", "exec", "--user", Constants.MYSQL_USER, "-d",
                containerName.trim(),
                "sh", "-lc",
                "mysqld --datadir=" + shellQuote(dataDir)
                        + " --port=" + port
                        + " --socket=" + shellQuote(socketPath)
                        + " --pid-file=" + shellQuote(pidPath)
                        + " --log-error=" + shellQuote(logPath)
                        + " --bind-address=0.0.0.0"
                        + " --innodb-stats-persistent=ON"
                        + " --innodb-stats-auto-recalc=OFF"
                        + " --innodb-stats-persistent-sample-pages="
                        + Constants.MYSQL_INNODB_STATS_PERSISTENT_SAMPLE_PAGES
                        + " --eq-range-index-dive-limit=0"
        );
    }

    private List<String> stopMysqlProcessCommand(String containerName, String socketPath, String rootPassword) {
        return List.of(
                "docker", "exec",
                containerName.trim(),
                "sh", "-lc",
                "mysqladmin --socket=" + shellQuote(socketPath) + " -uroot -p" + shellQuote(rootPassword) + " shutdown"
        );
    }

    private String templateMountPath(String dbmsName, String templateVersion) {
        return mountPath("templates", dbmsName, templateVersion, "data");
    }

    private String templateLogPath(String dbmsName, String templateVersion) {
        return mountPath("templates", dbmsName, templateVersion, "logs");
    }

    private String evalMountPath(String dbmsName, String environmentId) {
        return mountPath("evals", dbmsName, environmentId, "data");
    }

    private String evalLogPath(String dbmsName, String environmentId) {
        return mountPath("evals", dbmsName, environmentId, "logs");
    }

    private String mountPath(String area, String dbmsName, String scriptName, String leaf) {
        return Path.of(options.getMountRoot(), area, dbmsName, scriptName, leaf)
                .normalize()
                .toString();
    }

    private String mysqlSocketPath(String environmentId) {
        return "/tmp/mysql-" + environmentId + ".sock";
    }

    private String mysqlPidPath(String environmentId) {
        return "/tmp/mysql-" + environmentId + ".pid";
    }

    private String mysqlTemplateSocketPath(String templateVersion) {
        return "/tmp/mysql-template-" + templateVersion + ".sock";
    }

    private String mysqlTemplatePidPath(String templateVersion) {
        return "/tmp/mysql-template-" + templateVersion + ".pid";
    }

    private String scriptName(String value) {
        return Names.scriptName(value.trim());
    }

    private String shellQuote(String value) {
        return "'" + (value != null ? value : "").replace("'", "'\"'\"'") + "'";
    }
}
