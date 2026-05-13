package com.quertimizer.judge.application.port.out;

import com.quertimizer.judge.application.model.DatabaseSlot;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.domain.model.QueuePriority;
import com.quertimizer.judge.domain.model.QueueStatusListener;

public interface LvmSnapshotPort {

    DatabaseSlot acquire(DbmsType dbmsType, QueuePriority priority, QueueStatusListener listener);

    void release(DatabaseSlot slot);

    void createMaintenanceTemplate(String scriptDbmsName, String templateVersion);

    void prepareTemplateLog(String scriptDbmsName, String templateVersion);

    void sealTemplate(String scriptDbmsName, String templateVersion);

    void dropTemplate(String scriptDbmsName, String templateVersion);

    String evalLvName(String scriptDbmsName, String environmentScriptName);

    String listEvalSnapshotNames();

    void createEvalSnapshot(String scriptDbmsName, String templateVersion, String environmentScriptName);

    void dropEvalSnapshot(String scriptDbmsName, String environmentScriptName);

    DbmsType resolveEvalDbmsType(String evalLvName);

    String resolveEvalEnvironmentName(String evalLvName);

    void dropOrphanEvalSnapshot(String evalLvName);
}
