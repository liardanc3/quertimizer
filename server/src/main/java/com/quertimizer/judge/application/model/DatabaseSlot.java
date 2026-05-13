package com.quertimizer.judge.application.model;

import lombok.Data;

@Data
public class DatabaseSlot {

    private final DatabaseLease databaseLease;
    private final int port;

    public DatabaseSlot(DatabaseLease databaseLease, int port) {
        this.databaseLease = databaseLease;
        this.port = port;
    }

    public Database getDatabase() {
        return databaseLease.getDatabase();
    }
}
