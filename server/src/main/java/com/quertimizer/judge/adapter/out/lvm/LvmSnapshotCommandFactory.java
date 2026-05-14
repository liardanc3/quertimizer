package com.quertimizer.judge.adapter.out.lvm;

import com.quertimizer.judge.application.model.Constants;
import com.quertimizer.judge.application.model.Options;
import com.quertimizer.judge.application.model.Names;
import com.quertimizer.judge.domain.model.DbmsType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.quertimizer.judge.domain.model.JudgeFailReason.UNKNOWN_LVM_EVAL_SNAPSHOT_NAME;

@Component
@RequiredArgsConstructor
public class LvmSnapshotCommandFactory {

    private final Options options;

    public List<List<String>> createMaintenanceTemplateCommands(String dbmsName, String sourceTemplateVersion,
                                                                String nextTemplateVersion) {
        String normalizedDbmsName = scriptName(dbmsName);
        String sourceVersion = scriptName(sourceTemplateVersion);
        String nextVersion = scriptName(nextTemplateVersion);
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

    public List<List<String>> prepareTemplateLogCommands(String dbmsName, String templateVersion) {
        // 템플릿 로그 디렉터리 생성과 DB 프로세스 소유권 부여 명령 반환
        String normalizedDbmsName = scriptName(dbmsName);
        String normalizedTemplateVersion = scriptName(templateVersion);
        String templateLogPath = templateLogPath(normalizedDbmsName, normalizedTemplateVersion);
        return List.of(
                sudoCommand("mkdir", "-p", templateLogPath),
                sudoCommand("chown", "-R", Constants.DATABASE_PROCESS_OWNER, templateLogPath)
        );
    }

    public List<List<String>> sealTemplateCommands(String dbmsName, String templateVersion) {
        // 템플릿 식별자 정규화 후 sync와 unmount 명령 반환
        String normalizedDbmsName = scriptName(dbmsName);
        String normalizedTemplateVersion = scriptName(templateVersion);
        return List.of(
                sudoCommand("sync"),
                unmountCommand(templateMountPath(normalizedDbmsName, normalizedTemplateVersion))
        );
    }

    public List<List<String>> dropTemplateCommands(String dbmsName, String templateVersion) {
        // mount 정리와 LV 제거 명령 반환
        String normalizedDbmsName = scriptName(dbmsName);
        String normalizedTemplateVersion = scriptName(templateVersion);
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
        String normalizedDbmsName = scriptName(dbmsName);
        String normalizedTemplateVersion = scriptName(templateVersion);
        String normalizedEnvironmentId = scriptName(environmentId);
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
        commands.add(sudoCommand("chown", "-R", Constants.DATABASE_PROCESS_OWNER, evalLogPath));

        return List.copyOf(commands);
    }

    public List<List<String>> dropEvalSnapshotCommands(String dbmsName, String environmentId) {
        // mount 정리와 LV 제거 명령 반환
        String normalizedDbmsName = scriptName(dbmsName);
        String normalizedEnvironmentId = scriptName(environmentId);
        String mountPath = evalMountPath(normalizedDbmsName, normalizedEnvironmentId);
        return List.of(
                shellCommand("if mountpoint -q " + shellQuote(mountPath) + "; then sudo -n umount "
                        + shellQuote(mountPath) + "; fi"),
                sudoCommand("lvremove", "-y", lvPath(evalLvName(normalizedDbmsName, normalizedEnvironmentId)))
        );
    }

    public List<String> listEvalSnapshotNamesCommand() {
        // 평가 LV 이름 목록 조회 명령 반환
        return shellCommand("sudo -n lvs --noheadings -o lv_name " + shellQuote(options.getVolumeGroup())
                + " 2>/dev/null | awk '{$1=$1; print}' | grep '^eval_' || true");
    }

    public List<String> dropOrphanEvalSnapshotCommand(String evalLvName) {
        // 고아 평가 LV mount, LV, 로그 경로 제거 명령 반환
        String normalizedEvalLvName = scriptName(evalLvName);
        String dbmsName = resolveEvalDbmsName(normalizedEvalLvName);
        String environmentId = resolveEvalEnvironmentId(normalizedEvalLvName, dbmsName);
        String dataMountPath = evalMountPath(dbmsName, environmentId);
        String logMountPath = evalLogPath(dbmsName, environmentId);
        return shellCommand(
                "while mountpoint -q " + shellQuote(dataMountPath)
                        + "; do sudo -n umount " + shellQuote(dataMountPath) + " || exit 1; done; "
                        + "if sudo -n lvs --noheadings " + shellQuote(lvPath(normalizedEvalLvName))
                        + " >/dev/null 2>&1; then sudo -n lvremove -y " + shellQuote(lvPath(normalizedEvalLvName)) + "; fi; "
                        + "sudo -n rm -rf " + shellQuote(dataMountPath) + " " + shellQuote(logMountPath)
        );
    }

    public String evalLvNameForEnvironment(String dbmsName, String environmentId) {
        // 평가 LV 이름 생성
        return evalLvName(scriptName(dbmsName), scriptName(environmentId));
    }

    public DbmsType resolveEvalDbmsType(String evalLvName) {
        // 평가 LV 이름 기준 DBMS 유형 변환
        return switch (resolveEvalDbmsName(scriptName(evalLvName))) {
            case "postgresql" -> DbmsType.POSTGRESQL;
            case "mysql" -> DbmsType.MYSQL;
            default -> throw new IllegalArgumentException(UNKNOWN_LVM_EVAL_SNAPSHOT_NAME.format(evalLvName));
        };
    }

    public String resolveEvalEnvironmentName(String evalLvName) {
        // 평가 LV 이름 기준 실행 환경 스크립트 이름 추출
        String normalizedEvalLvName = scriptName(evalLvName);
        return resolveEvalEnvironmentId(normalizedEvalLvName, resolveEvalDbmsName(normalizedEvalLvName));
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
        String marker = "# quertimizer LVM snapshot docker bridge access";
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

    private String mountPath(String area, String dbmsName, String scriptName, String leaf) {
        return Path.of(options.getMountRoot(), area, dbmsName, scriptName, leaf)
                .normalize()
                .toString();
    }

    private String resolveEvalDbmsName(String evalLvName) {
        if (evalLvName.startsWith("eval_postgresql_")) {
            return "postgresql";
        }
        if (evalLvName.startsWith("eval_mysql_")) {
            return "mysql";
        }

        throw new IllegalArgumentException(UNKNOWN_LVM_EVAL_SNAPSHOT_NAME.format(evalLvName));
    }

    private String resolveEvalEnvironmentId(String evalLvName, String dbmsName) {
        return evalLvName.substring(("eval_" + dbmsName + "_").length());
    }

    private String scriptName(String value) {
        return Names.scriptName(value.trim());
    }

    private String shellQuote(String value) {
        return "'" + (value != null ? value : "").replace("'", "'\"'\"'") + "'";
    }
}
