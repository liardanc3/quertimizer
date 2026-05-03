package com.quertimizer.judge.adapter.out.execution;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.adapter.out.execution.JudgeRuntimeConstants;
import com.quertimizer.judge.adapter.out.execution.LvmSnapshotNameSupport;
import com.quertimizer.judge.adapter.out.execution.LvmSnapshotRuntimeOptions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LvmSnapshotRuntimeCommandFactory {

    private final LvmSnapshotRuntimeOptions options;

    public LvmSnapshotRuntimeCommandFactory(LvmSnapshotRuntimeOptions options) {
        this.options = Objects.requireNonNull(options, "LVM 스냅샷 런타임 옵션이 필요하다.");
    }

    public List<List<String>> createMaintenanceTemplateCommands(String dbmsName, String sourceTemplateVersion,
                                                                String nextTemplateVersion) {
        String normalizedDbmsName = runtimeName(dbmsName);
        String sourceVersion = runtimeName(sourceTemplateVersion);
        String nextVersion = runtimeName(nextTemplateVersion);
        String nextLvName = templateLvName(normalizedDbmsName, nextVersion);
        String templateMountPath = templateMountPath(normalizedDbmsName, nextVersion);

        List<List<String>> commands = new ArrayList<>();
        commands.add(sudoCommand("lvcreate", "-s", "-n", nextLvName, lvPath(templateLvName(normalizedDbmsName, sourceVersion))));
        commands.add(sudoCommand("lvchange", "-ay", "-K", lvPath(nextLvName)));
        commands.add(sudoCommand("mkdir", "-p", templateMountPath));
        commands.add(sudoCommand("mount", lvPath(nextLvName), templateMountPath));
        if ("postgresql".equals(normalizedDbmsName)) {
            commands.add(allowPostgresDockerBridgeCommand(templateMountPath));
        }

        return List.copyOf(commands);
    }

    public List<List<String>> sealTemplateCommands(String dbmsName, String templateVersion) {
        String normalizedDbmsName = runtimeName(dbmsName);
        String normalizedTemplateVersion = runtimeName(templateVersion);

        return List.of(
                sudoCommand("sync"),
                unmountCommand(templateMountPath(normalizedDbmsName, normalizedTemplateVersion))
        );
    }

    public List<List<String>> dropTemplateCommands(String dbmsName, String templateVersion) {
        String normalizedDbmsName = runtimeName(dbmsName);
        String normalizedTemplateVersion = runtimeName(templateVersion);
        String templateLvName = templateLvName(normalizedDbmsName, normalizedTemplateVersion);

        return List.of(
                unmountCommand(templateMountPath(normalizedDbmsName, normalizedTemplateVersion)),
                shellCommand("if sudo -n lvs --noheadings " + shellQuote(lvPath(templateLvName))
                        + " >/dev/null 2>&1; then sudo -n lvremove -y "
                        + shellQuote(lvPath(templateLvName)) + "; fi")
        );
    }

    public List<List<String>> createEvalSnapshotCommands(String dbmsName, String templateVersion,
                                                         String environmentId) {
        String normalizedDbmsName = runtimeName(dbmsName);
        String normalizedTemplateVersion = runtimeName(templateVersion);
        String normalizedEnvironmentId = runtimeName(environmentId);
        String evalLvName = evalLvName(normalizedDbmsName, normalizedEnvironmentId);
        String evalMountPath = evalMountPath(normalizedDbmsName, normalizedEnvironmentId);
        String evalLogPath = evalLogPath(normalizedDbmsName, normalizedEnvironmentId);

        List<List<String>> commands = new ArrayList<>();
        commands.add(sudoCommand("lvcreate", "-s", "-n", evalLvName,
                lvPath(templateLvName(normalizedDbmsName, normalizedTemplateVersion))));
        commands.add(sudoCommand("lvchange", "-ay", "-K", lvPath(evalLvName)));
        commands.add(sudoCommand("mkdir", "-p", evalMountPath, evalLogPath));
        commands.add(sudoCommand("mount", lvPath(evalLvName), evalMountPath));
        if ("postgresql".equals(normalizedDbmsName)) {
            commands.add(allowPostgresDockerBridgeCommand(evalMountPath));
        }
        commands.add(sudoCommand("chown", "-R", JudgeRuntimeConstants.JUDGE_RUNTIME_OWNER, evalLogPath));

        return List.copyOf(commands);
    }

    public List<List<String>> dropEvalSnapshotCommands(String dbmsName, String environmentId) {
        String normalizedDbmsName = runtimeName(dbmsName);
        String normalizedEnvironmentId = runtimeName(environmentId);
        String mountPath = evalMountPath(normalizedDbmsName, normalizedEnvironmentId);

        return List.of(
                shellCommand("if mountpoint -q " + shellQuote(mountPath) + "; then sudo -n umount "
                        + shellQuote(mountPath) + "; fi"),
                sudoCommand("lvremove", "-y", lvPath(evalLvName(normalizedDbmsName, normalizedEnvironmentId)))
        );
    }

    public List<String> startEvalProcessCommand(DbmsType dbmsType, String runnerContainer,
                                                String environmentId, int port) {
        String normalizedEnvironmentId = runtimeName(environmentId);
        return switch (Objects.requireNonNull(dbmsType, "DBMS 유형이 필요하다.")) {
            case POSTGRESQL -> startPostgresProcessCommand(
                    runnerContainer,
                    evalMountPath("postgresql", normalizedEnvironmentId),
                    evalLogPath("postgresql", normalizedEnvironmentId) + "/postgres.log",
                    port
            );
            case MYSQL -> startMysqlProcessCommand(
                    runnerContainer,
                    evalMountPath("mysql", normalizedEnvironmentId),
                    evalLogPath("mysql", normalizedEnvironmentId) + "/mysql.log",
                    mysqlSocketPath(normalizedEnvironmentId),
                    mysqlPidPath(normalizedEnvironmentId),
                    port
            );
        };
    }

    public List<String> stopEvalProcessCommand(DbmsType dbmsType, String runnerContainer,
                                               String environmentId, String mysqlRootPassword) {
        String normalizedEnvironmentId = runtimeName(environmentId);
        return switch (Objects.requireNonNull(dbmsType, "DBMS 유형이 필요하다.")) {
            case POSTGRESQL -> stopPostgresProcessCommand(
                    runnerContainer,
                    evalMountPath("postgresql", normalizedEnvironmentId)
            );
            case MYSQL -> stopMysqlProcessCommand(
                    runnerContainer,
                    mysqlSocketPath(normalizedEnvironmentId),
                    mysqlRootPassword
            );
        };
    }

    public List<List<String>> startTemplateProcessCommands(DbmsType dbmsType, String runnerContainer,
                                                           String templateVersion, int port) {
        String normalizedTemplateVersion = runtimeName(templateVersion);
        return switch (Objects.requireNonNull(dbmsType, "DBMS 유형이 필요하다.")) {
            case POSTGRESQL -> List.of(
                    sudoCommand("mkdir", "-p", templateLogPath("postgresql", normalizedTemplateVersion)),
                    sudoCommand("chown", "-R", JudgeRuntimeConstants.JUDGE_RUNTIME_OWNER, templateLogPath("postgresql", normalizedTemplateVersion)),
                    startPostgresProcessCommand(
                            runnerContainer,
                            templateMountPath("postgresql", normalizedTemplateVersion),
                            templateLogPath("postgresql", normalizedTemplateVersion) + "/postgres.log",
                            port
                    )
            );
            case MYSQL -> List.of(
                    sudoCommand("mkdir", "-p", templateLogPath("mysql", normalizedTemplateVersion)),
                    sudoCommand("chown", "-R", JudgeRuntimeConstants.JUDGE_RUNTIME_OWNER, templateLogPath("mysql", normalizedTemplateVersion)),
                    startMysqlProcessCommand(
                            runnerContainer,
                            templateMountPath("mysql", normalizedTemplateVersion),
                            templateLogPath("mysql", normalizedTemplateVersion) + "/mysql.log",
                            mysqlTemplateSocketPath(normalizedTemplateVersion),
                            mysqlTemplatePidPath(normalizedTemplateVersion),
                            port
                    )
            );
        };
    }

    public List<String> stopTemplateProcessCommand(DbmsType dbmsType, String runnerContainer,
                                                   String templateVersion, String mysqlRootPassword) {
        String normalizedTemplateVersion = runtimeName(templateVersion);
        return switch (Objects.requireNonNull(dbmsType, "DBMS 유형이 필요하다.")) {
            case POSTGRESQL -> stopPostgresProcessCommand(
                    runnerContainer,
                    templateMountPath("postgresql", normalizedTemplateVersion)
            );
            case MYSQL -> stopMysqlProcessCommand(
                    runnerContainer,
                    mysqlTemplateSocketPath(normalizedTemplateVersion),
                    mysqlRootPassword
            );
        };
    }

    private List<String> startPostgresProcessCommand(String runnerContainer, String dataDir,
                                                     String logPath, int port) {
        return List.of(
                "docker", "exec", "--user", JudgeRuntimeConstants.POSTGRES_USER, "-d",
                requireText(runnerContainer, "runnerContainer"),
                "sh", "-lc",
                JudgeRuntimeConstants.POSTGRES_CTL_RESOLVER
                        + "\"$pg_ctl_bin\" -D " + shellQuote(dataDir)
                        + " -o " + shellQuote("-p " + port + " -c listen_addresses=0.0.0.0")
                        + " -l " + shellQuote(logPath) + " start"
        );
    }

    private List<String> stopPostgresProcessCommand(String runnerContainer, String dataDir) {
        return List.of(
                "docker", "exec", "--user", JudgeRuntimeConstants.POSTGRES_USER,
                requireText(runnerContainer, "runnerContainer"),
                "sh", "-lc",
                JudgeRuntimeConstants.POSTGRES_CTL_RESOLVER
                        + "\"$pg_ctl_bin\" -D " + shellQuote(dataDir) + " -m fast stop"
        );
    }

    private List<String> startMysqlProcessCommand(String runnerContainer, String dataDir, String logPath,
                                                  String socketPath, String pidPath, int port) {
        return List.of(
                "docker", "exec", "--user", JudgeRuntimeConstants.MYSQL_USER, "-d",
                requireText(runnerContainer, "runnerContainer"),
                "sh", "-lc",
                "mysqld --datadir=" + shellQuote(dataDir)
                        + " --port=" + port
                        + " --socket=" + shellQuote(socketPath)
                        + " --pid-file=" + shellQuote(pidPath)
                        + " --log-error=" + shellQuote(logPath)
                        + " --bind-address=0.0.0.0"
        );
    }

    private List<String> stopMysqlProcessCommand(String runnerContainer, String socketPath, String rootPassword) {
        return List.of(
                "docker", "exec",
                requireText(runnerContainer, "runnerContainer"),
                "sh", "-lc",
                "mysqladmin --socket=" + shellQuote(socketPath) + " -uroot -p" + shellQuote(rootPassword) + " shutdown"
        );
    }

    private List<String> sudoCommand(String... command) {
        List<String> arguments = new ArrayList<>(command.length + 2);
        arguments.add("sudo");
        arguments.add("-n");
        arguments.addAll(List.of(command));
        return List.copyOf(arguments);
    }

    private List<String> shellCommand(String command) {
        return List.of("sh", "-lc", command);
    }

    private List<String> unmountCommand(String mountPath) {
        return shellCommand("while mountpoint -q " + shellQuote(mountPath)
                + "; do sudo -n umount " + shellQuote(mountPath) + " || exit 1; done");
    }

    private List<String> allowPostgresDockerBridgeCommand(String dataDir) {
        String hbaPath = Path.of(dataDir, "pg_hba.conf").normalize().toString();
        String marker = "# quertimizer lvm snapshot docker bridge access";
        String rule = "host all judge 172.16.0.0/12 md5";

        return shellCommand("if sudo -n test -f " + shellQuote(hbaPath)
                + " && ! sudo -n grep -q " + shellQuote(marker) + " " + shellQuote(hbaPath)
                + "; then printf '%s\\n' '' " + shellQuote(marker) + " " + shellQuote(rule)
                + " | sudo -n tee -a " + shellQuote(hbaPath) + " >/dev/null; fi");
    }

    private String templateLvName(String dbmsName, String templateVersion) {
        return "tpl_" + dbmsName + "_" + templateVersion;
    }

    private String evalLvName(String dbmsName, String environmentId) {
        return "eval_" + dbmsName + "_" + environmentId;
    }

    private String lvPath(String lvName) {
        return "/dev/" + options.getVolumeGroup() + "/" + lvName;
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

    private String mountPath(String area, String dbmsName, String runtimeName, String leaf) {
        return Path.of(options.getMountRoot(), area, dbmsName, runtimeName, leaf)
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

    private String runtimeName(String value) {
        return LvmSnapshotNameSupport.scriptName(requireText(value, "런타임 이름"));
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "이 비어 있다.");
        }

        return value.trim();
    }

    private String shellQuote(String value) {
        return "'" + (value != null ? value : "").replace("'", "'\"'\"'") + "'";
    }
}
